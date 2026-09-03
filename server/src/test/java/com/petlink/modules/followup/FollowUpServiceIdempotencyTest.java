package com.petlink.modules.followup;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.modules.followup.dto.SubmitFollowUpRequest;
import com.petlink.modules.followup.entity.FollowUpRecord;
import com.petlink.modules.followup.mapper.FollowUpRecordMapper;
import com.petlink.modules.followup.service.FollowUpQueryService;
import com.petlink.modules.followup.service.FollowUpRequestNormalizer;
import com.petlink.modules.followup.service.FollowUpService;
import com.petlink.modules.followup.service.FollowUpTransactionalService;
import com.petlink.modules.followup.vo.FollowUpRecordResponse;
import com.petlink.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FollowUpServiceIdempotencyTest {
    FollowUpRecordMapper mapper; FollowUpTransactionalService tx; FollowUpQueryService query; FollowUpService service;
    final FollowUpRequestNormalizer normalizer=new FollowUpRequestNormalizer(); final UserPrincipal user=new UserPrincipal(1001L,"USER");
    static final String KEY="61f3576a-432f-4e08-8b69-642d3a6776d9";

    @BeforeEach void setUp(){mapper=mock(FollowUpRecordMapper.class);tx=mock(FollowUpTransactionalService.class);query=mock(FollowUpQueryService.class);service=new FollowUpService(mapper,normalizer,tx,query);}

    @Test void existingSameKeyReplaysBeforeOldImageTokensAreValidated(){FollowUpRecord f=record(8001L,7001L);when(mapper.selectBySubmitterAndKey(1001L,KEY)).thenReturn(f);when(query.replay(1001L,KEY)).thenReturn(response("8001"));SubmitFollowUpRequest r=req();r.setImageTokens(List.of("this-is-not-a-token"));var out=service.submit(user,7001L,r);assertFalse(out.isCreated());assertEquals("8001",out.getResponse().getId());verify(tx,never()).create(any(),anyLong(),any());}
    @Test void existingKeyForDifferentRecordIs40908(){when(mapper.selectBySubmitterAndKey(1001L,KEY)).thenReturn(record(8001L,7001L));BusinessException ex=assertThrows(BusinessException.class,()->service.submit(user,7002L,req()));assertEquals(ErrorCode.IDEMPOTENCY_KEY_CONFLICT,ex.getErrorCode());verify(tx,never()).create(any(),anyLong(),any());}
    @Test void newRequestCreatesAndMarks201Result(){when(mapper.selectBySubmitterAndKey(1001L,KEY)).thenReturn(null);when(tx.create(any(),eq(7001L),any())).thenReturn(response("8001"));var out=service.submit(user,7001L,req());assertTrue(out.isCreated());assertEquals("8001",out.getResponse().getId());}
    @Test void namedUniqueRaceRecoversSameRecord(){when(mapper.selectBySubmitterAndKey(1001L,KEY)).thenReturn(null,record(8001L,7001L));when(tx.create(any(),eq(7001L),any())).thenThrow(new DataIntegrityViolationException("Duplicate entry for key 'uk_follow_up_submitter_idempotency'"));when(query.replay(1001L,KEY)).thenReturn(response("8001"));var out=service.submit(user,7001L,req());assertFalse(out.isCreated());}
    @Test void namedUniqueRaceDifferentRecordReturns40908(){when(mapper.selectBySubmitterAndKey(1001L,KEY)).thenReturn(null,record(8001L,7002L));when(tx.create(any(),eq(7001L),any())).thenThrow(new DataIntegrityViolationException("uk_follow_up_submitter_idempotency"));assertEquals(ErrorCode.IDEMPOTENCY_KEY_CONFLICT,assertThrows(BusinessException.class,()->service.submit(user,7001L,req())).getErrorCode());}
    @Test void unrelatedDatabaseConstraintIsNotMisreportedAsReplay(){when(mapper.selectBySubmitterAndKey(1001L,KEY)).thenReturn(null);when(tx.create(any(),eq(7001L),any())).thenThrow(new DataIntegrityViolationException("fk_follow_up_submitter"));assertThrows(DataIntegrityViolationException.class,()->service.submit(user,7001L,req()));}
    @Test void adminCannotSubmitFollowUp(){UserPrincipal admin=new UserPrincipal(9001L,"ADMIN");assertEquals(ErrorCode.FORBIDDEN,assertThrows(BusinessException.class,()->service.submit(admin,7001L,req())).getErrorCode());verify(mapper,never()).selectBySubmitterAndKey(anyLong(),anyString());}

    private SubmitFollowUpRequest req(){SubmitFollowUpRequest r=new SubmitFollowUpRequest();r.setIdempotencyKey(KEY);r.setContent("ok");r.setImageTokens(List.of());return r;}
    private FollowUpRecord record(Long id,Long adoptionId){FollowUpRecord f=new FollowUpRecord();f.setId(id);f.setAdoptionRecordId(adoptionId);f.setSubmitterId(1001L);f.setIdempotencyKey(KEY);return f;}
    private FollowUpRecordResponse response(String id){return new FollowUpRecordResponse(id,"7001","1001","ok",null,List.of(),OffsetDateTime.now());}
}

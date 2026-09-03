package com.petlink.modules.followup;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.modules.adoption.entity.AdoptionRecord;
import com.petlink.modules.adoption.mapper.AdoptionRecordMapper;
import com.petlink.modules.followup.entity.FollowUpRecord;
import com.petlink.modules.followup.mapper.FollowUpRecordMapper;
import com.petlink.modules.followup.service.FollowUpFileBindingService;
import com.petlink.modules.followup.service.FollowUpRequestNormalizer;
import com.petlink.modules.followup.service.FollowUpResponseAssembler;
import com.petlink.modules.followup.service.FollowUpTransactionalService;
import com.petlink.modules.followup.vo.FollowUpRecordResponse;
import com.petlink.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FollowUpTransactionalServiceTest {
    AdoptionRecordMapper adoptions; FollowUpRecordMapper records; FollowUpFileBindingService files; FollowUpResponseAssembler assembler; FollowUpTransactionalService service;
    final UserPrincipal user=new UserPrincipal(1001L,"USER");
    final FollowUpRequestNormalizer.NormalizedSubmit normalized=new FollowUpRequestNormalizer.NormalizedSubmit("61f3576a-432f-4e08-8b69-642d3a6776d9","content","health",List.of("550e8400-e29b-41d4-a716-446655440030"));

    @BeforeEach void setUp(){adoptions=mock(AdoptionRecordMapper.class);records=mock(FollowUpRecordMapper.class);files=mock(FollowUpFileBindingService.class);assembler=mock(FollowUpResponseAssembler.class);service=new FollowUpTransactionalService(adoptions,records,files,assembler);}

    @Test void ownerCheckHappensBeforeFollowUpInsert(){when(adoptions.selectById(7001L)).thenReturn(adoption(7001L,1001L));when(records.insert(any())).thenAnswer(i->{FollowUpRecord f=i.getArgument(0);f.setId(8001L);return 1;});FollowUpFileBindingService.PreparedBindings prepared=mock(FollowUpFileBindingService.PreparedBindings.class);when(files.lockAndValidate(1001L,normalized.getImageTokens())).thenReturn(prepared);FollowUpRecord saved=record();when(records.selectById(8001L)).thenReturn(saved);when(assembler.record(saved)).thenReturn(response());service.create(user,7001L,normalized);InOrder order=inOrder(adoptions,records,files);order.verify(adoptions).selectById(7001L);order.verify(records).insert(any());order.verify(files).lockAndValidate(1001L,normalized.getImageTokens());order.verify(files).bindPrepared(1001L,8001L,normalized.getImageTokens(),prepared);order.verify(files).copyPrepared(prepared);}
    @Test void otherUsersAdoptionRecordIsHiddenAs404(){when(adoptions.selectById(7001L)).thenReturn(adoption(7001L,2002L));BusinessException ex=assertThrows(BusinessException.class,()->service.create(user,7001L,normalized));assertEquals(ErrorCode.RESOURCE_NOT_FOUND,ex.getErrorCode());verify(records,never()).insert(any());verify(files,never()).lockAndValidate(anyLong(),anyList());}
    @Test void insertUsesAuthenticatedSubmitterAndUrlRecord(){when(adoptions.selectById(7001L)).thenReturn(adoption(7001L,1001L));when(records.insert(any())).thenAnswer(i->{FollowUpRecord f=i.getArgument(0);f.setId(8001L);return 1;});FollowUpFileBindingService.PreparedBindings prepared=mock(FollowUpFileBindingService.PreparedBindings.class);when(files.lockAndValidate(anyLong(),anyList())).thenReturn(prepared);FollowUpRecord saved=record();when(records.selectById(8001L)).thenReturn(saved);when(assembler.record(saved)).thenReturn(response());service.create(user,7001L,normalized);verify(records).insert(argThat(f->f.getAdoptionRecordId().equals(7001L)&&f.getSubmitterId().equals(1001L)&&normalized.getIdempotencyKey().equals(f.getIdempotencyKey())&&"content".equals(f.getContent())&&"health".equals(f.getHealthCondition())));}
    @Test void temporaryFilesAreNotTouchedWhenRecordInsertFails(){when(adoptions.selectById(7001L)).thenReturn(adoption(7001L,1001L));when(records.insert(any())).thenThrow(new org.springframework.dao.DataIntegrityViolationException("uk_follow_up_submitter_idempotency"));assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->service.create(user,7001L,normalized));verify(files,never()).lockAndValidate(anyLong(),anyList());}

    private AdoptionRecord adoption(Long id,Long userId){AdoptionRecord r=new AdoptionRecord();r.setId(id);r.setUserId(userId);r.setAnimalId(5001L);return r;}
    private FollowUpRecord record(){FollowUpRecord f=new FollowUpRecord();f.setId(8001L);f.setAdoptionRecordId(7001L);f.setSubmitterId(1001L);return f;}
    private FollowUpRecordResponse response(){return new FollowUpRecordResponse("8001","7001","1001","content","health",List.of(),OffsetDateTime.now());}
}

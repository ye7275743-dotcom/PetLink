package com.petlink.modules.followup;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.modules.adoption.entity.AdoptionRecord;
import com.petlink.modules.followup.entity.FollowUpRecord;
import com.petlink.modules.followup.mapper.FollowUpRecordMapper;
import com.petlink.modules.followup.service.FollowUpAccessService;
import com.petlink.modules.followup.service.FollowUpQueryService;
import com.petlink.modules.followup.service.FollowUpResponseAssembler;
import com.petlink.modules.followup.vo.FollowUpRecordResponse;
import com.petlink.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FollowUpQueryServiceTest {
    FollowUpRecordMapper mapper; FollowUpAccessService access; FollowUpResponseAssembler assembler; FollowUpQueryService service;
    final UserPrincipal user=new UserPrincipal(1001L,"USER"),admin=new UserPrincipal(9001L,"ADMIN"),rescuer=new UserPrincipal(3001L,"RESCUER");
    @BeforeEach void setUp(){mapper=mock(FollowUpRecordMapper.class);access=mock(FollowUpAccessService.class);assembler=mock(FollowUpResponseAssembler.class);service=new FollowUpQueryService(mapper,access,assembler);}

    @Test void invisibleAdoptionHistoryReturns404(){when(access.visibleAdoptionRecord(user,7001L)).thenReturn(null);assertEquals(ErrorCode.RESOURCE_NOT_FOUND,assertThrows(BusinessException.class,()->service.byAdoptionRecord(user,7001L)).getErrorCode());verify(mapper,never()).selectByAdoptionRecordAsc(anyLong());}
    @Test void adoptionHistoryPreservesMapperAscendingOrder(){AdoptionRecord a=new AdoptionRecord();a.setId(7001L);when(access.visibleAdoptionRecord(user,7001L)).thenReturn(a);FollowUpRecord f1=record(1L),f2=record(2L);when(mapper.selectByAdoptionRecordAsc(7001L)).thenReturn(List.of(f1,f2));when(assembler.record(f1)).thenReturn(response("1"));when(assembler.record(f2)).thenReturn(response("2"));assertEquals(List.of("1","2"),service.byAdoptionRecord(user,7001L).stream().map(FollowUpRecordResponse::getId).toList());}
    @Test void detailUsesAdoptionVisibilityAndHidesUnauthorized(){FollowUpRecord f=record(8001L);when(mapper.selectById(8001L)).thenReturn(f);when(access.visibleAdoptionRecord(user,7001L)).thenReturn(null);assertEquals(ErrorCode.RESOURCE_NOT_FOUND,assertThrows(BusinessException.class,()->service.detail(user,8001L)).getErrorCode());}
    @Test void adminPageUsesAllFrozenFilters(){when(mapper.selectAdminPage(5001L,1001L,7001L,20,0)).thenReturn(List.of());when(mapper.countAdmin(5001L,1001L,7001L)).thenReturn(0L);var p=service.adminPage(admin,1,20,5001L,1001L,7001L);assertEquals(0,p.getTotal());verify(mapper).selectAdminPage(5001L,1001L,7001L,20,0);}
    @Test void rescuerPageIsScopedByAuthenticatedRescuer(){when(access.isResponsibleAnimal(rescuer,5001L)).thenReturn(true);when(mapper.selectRescuerPage(3001L,5001L,20,0)).thenReturn(List.of());when(mapper.countRescuer(3001L,5001L)).thenReturn(0L);service.rescuerPage(rescuer,1,20,5001L);verify(mapper).selectRescuerPage(3001L,5001L,20,0);}
    @Test void nonRescuerCannotUseRescuerPage(){assertEquals(ErrorCode.FORBIDDEN,assertThrows(BusinessException.class,()->service.rescuerPage(user,1,20,null)).getErrorCode());}

    private FollowUpRecord record(Long id){FollowUpRecord f=new FollowUpRecord();f.setId(id);f.setAdoptionRecordId(7001L);f.setSubmitterId(1001L);return f;}
    private FollowUpRecordResponse response(String id){return new FollowUpRecordResponse(id,"7001","1001","ok",null,List.of(),OffsetDateTime.now());}
}

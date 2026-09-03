package com.petlink.modules.adoption;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.infrastructure.audit.service.OperationLogService;
import com.petlink.modules.adoption.dto.AdoptionAuditRequest;
import com.petlink.modules.adoption.dto.SubmitAdoptionApplicationRequest;
import com.petlink.modules.adoption.entity.AdoptionApplication;
import com.petlink.modules.adoption.entity.AdoptionRecord;
import com.petlink.modules.adoption.mapper.AdoptionApplicationMapper;
import com.petlink.modules.adoption.mapper.AdoptionRecordMapper;
import com.petlink.modules.adoption.service.AdoptionResponseAssembler;
import com.petlink.modules.adoption.service.AdoptionTransactionalService;
import com.petlink.modules.animal.entity.Animal;
import com.petlink.modules.animal.mapper.AnimalMapper;
import com.petlink.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdoptionTransactionalServiceTest {
    AdoptionApplicationMapper apps; AdoptionRecordMapper records; AnimalMapper animals; OperationLogService logs; AdoptionResponseAssembler assembler; AdoptionTransactionalService service;
    final UserPrincipal user=new UserPrincipal(1001L,"USER"); final UserPrincipal admin=new UserPrincipal(9001L,"ADMIN");

    @BeforeEach void setUp(){
        apps=mock(AdoptionApplicationMapper.class); records=mock(AdoptionRecordMapper.class); animals=mock(AnimalMapper.class); logs=mock(OperationLogService.class); assembler=mock(AdoptionResponseAssembler.class);
        service=new AdoptionTransactionalService(apps,records,animals,logs,assembler);
    }

    @Test void submitLocksAvailableAnimalCreatesPendingAndLog(){
        when(animals.selectForUpdate(5001L)).thenReturn(animal(5001L,"AVAILABLE"));
        when(apps.insert(any())).thenAnswer(i->{AdoptionApplication a=i.getArgument(0);a.setId(6001L);return 1;});
        when(apps.selectById(6001L)).thenReturn(app(6001L,1001L,5001L,"PENDING"));
        service.submit(user,5001L,request());
        verify(animals).selectForUpdate(5001L);
        verify(apps).insert(argThat(a->a.getUserId().equals(1001L)&&a.getAnimalId().equals(5001L)&&"PENDING".equals(a.getStatus())&&"reason".equals(a.getAdoptionReason())));
        verify(logs).append("ADOPTION_APPLICATION",6001L,"CREATE",null,"PENDING",1001L,null);
    }

    @Test void submitUnavailableReturnsStateConflict(){
        when(animals.selectForUpdate(5001L)).thenReturn(animal(5001L,"SUSPENDED"));
        BusinessException ex=assertThrows(BusinessException.class,()->service.submit(user,5001L,request()));
        assertEquals(ErrorCode.BUSINESS_STATE_CONFLICT,ex.getErrorCode()); verify(apps,never()).insert(any());
    }

    @Test void submitNamedLifetimeUniqueConstraintMaps40907Only(){
        when(animals.selectForUpdate(5001L)).thenReturn(animal(5001L,"AVAILABLE"));
        when(apps.insert(any())).thenThrow(new DataIntegrityViolationException("Duplicate entry for key 'uk_adoption_application_user_animal'"));
        BusinessException ex=assertThrows(BusinessException.class,()->service.submit(user,5001L,request()));
        assertEquals(ErrorCode.ADOPTION_APPLICATION_ALREADY_EXISTS,ex.getErrorCode());
    }

    @Test void submitOtherConstraintIsNotMisreportedAsDuplicateApplication(){
        when(animals.selectForUpdate(5001L)).thenReturn(animal(5001L,"AVAILABLE"));
        when(apps.insert(any())).thenThrow(new DataIntegrityViolationException("constraint fk_something_else"));
        assertThrows(DataIntegrityViolationException.class,()->service.submit(user,5001L,request()));
    }

    @Test void withdrawUsesConditionalOwnerPendingUpdateAndLogs(){
        when(apps.withdraw(eq(6001L),eq(1001L),any())).thenReturn(1);
        AdoptionApplication saved=app(6001L,1001L,5001L,"WITHDRAWN"); saved.setUpdatedAt(LocalDateTime.now()); when(apps.selectById(6001L)).thenReturn(saved);
        var out=service.withdraw(user,6001L); assertEquals("WITHDRAWN",out.getStatus());
        verify(logs).append("ADOPTION_APPLICATION",6001L,"WITHDRAW","PENDING","WITHDRAWN",1001L,null);
    }

    @Test void withdrawOtherUsersApplicationIs404(){
        when(apps.withdraw(eq(6001L),eq(1001L),any())).thenReturn(0); when(apps.selectById(6001L)).thenReturn(app(6001L,2002L,5001L,"PENDING"));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND,assertThrows(BusinessException.class,()->service.withdraw(user,6001L)).getErrorCode());
    }

    @Test void rejectLocksOnlyTargetAndUsesRejectReason(){
        when(apps.selectForUpdate(6001L)).thenReturn(app(6001L,1001L,5001L,"PENDING")); when(apps.rejectPending(eq(6001L),eq(9001L),eq("not suitable"),any())).thenReturn(1);
        when(apps.selectById(6001L)).thenReturn(app(6001L,1001L,5001L,"REJECTED"));
        AdoptionAuditRequest r=audit("REJECT"," not suitable "); service.audit(admin,6001L,r);
        verify(animals,never()).selectForUpdate(anyLong());
        verify(logs).append("ADOPTION_APPLICATION",6001L,"AUDIT_REJECT","PENDING","REJECTED",9001L,"not suitable");
    }

    @Test void rejectHistoricalOwnApplicationIs403AndNoLog(){
        when(apps.selectForUpdate(6001L)).thenReturn(app(6001L,9001L,5001L,"PENDING"));
        BusinessException ex=assertThrows(BusinessException.class,()->service.audit(admin,6001L,audit("REJECT","reason")));
        assertEquals(ErrorCode.FORBIDDEN,ex.getErrorCode()); verify(logs,never()).append(anyString(),anyLong(),anyString(),any(),any(),anyLong(),any());
    }

    @Test void approveUsesFrozenLockOrderCreatesRecordAdoptsAndInvalidates(){
        AdoptionApplication target=app(6001L,1001L,5001L,"PENDING"); AdoptionApplication other1=app(6002L,1002L,5001L,"PENDING"); AdoptionApplication other2=app(6003L,1003L,5001L,"PENDING");
        when(apps.selectAnimalIdById(6001L)).thenReturn(5001L); when(animals.selectForUpdate(5001L)).thenReturn(animal(5001L,"AVAILABLE")); when(apps.selectForUpdate(6001L)).thenReturn(target);
        when(apps.selectOtherPendingForUpdate(5001L,6001L)).thenReturn(List.of(other1,other2)); when(apps.approvePending(eq(6001L),eq(9001L),any())).thenReturn(1);
        when(records.insert(any())).thenAnswer(i->{AdoptionRecord r=i.getArgument(0);r.setId(7001L);return 1;}); when(animals.adoptAvailable(eq(5001L),any())).thenReturn(1);
        when(apps.invalidatePending(eq(6002L),any())).thenReturn(1); when(apps.invalidatePending(eq(6003L),any())).thenReturn(1);
        when(apps.selectById(6001L)).thenReturn(app(6001L,1001L,5001L,"APPROVED")); when(records.selectById(7001L)).thenReturn(record(7001L,6001L,5001L,1001L));
        service.audit(admin,6001L,auditApprove());
        InOrder order=inOrder(apps,animals);
        order.verify(apps).selectAnimalIdById(6001L); order.verify(animals).selectForUpdate(5001L); order.verify(apps).selectForUpdate(6001L); order.verify(apps).selectOtherPendingForUpdate(5001L,6001L);
        verify(records).insert(argThat(r->r.getApplicationId().equals(6001L)&&r.getAnimalId().equals(5001L)&&r.getUserId().equals(1001L)));
        verify(animals).adoptAvailable(eq(5001L),any()); verify(apps).invalidatePending(eq(6002L),any()); verify(apps).invalidatePending(eq(6003L),any());
        verify(logs).append("ADOPTION_APPLICATION",6001L,"AUDIT_APPROVE","PENDING","APPROVED",9001L,null);
        verify(logs).append("ANIMAL",5001L,"ADOPT","AVAILABLE","ADOPTED",9001L,null);
        verify(logs).append("ADOPTION_APPLICATION",6002L,"AUTO_INVALIDATE","PENDING","INVALIDATED",9001L,null);
        verify(logs).append("ADOPTION_APPLICATION",6003L,"AUTO_INVALIDATE","PENDING","INVALIDATED",9001L,null);
    }

    @Test void approveHistoricalOwnApplicationIs403BeforeWrites(){
        when(apps.selectAnimalIdById(6001L)).thenReturn(5001L); when(animals.selectForUpdate(5001L)).thenReturn(animal(5001L,"AVAILABLE")); when(apps.selectForUpdate(6001L)).thenReturn(app(6001L,9001L,5001L,"PENDING"));
        assertEquals(ErrorCode.FORBIDDEN,assertThrows(BusinessException.class,()->service.audit(admin,6001L,auditApprove())).getErrorCode());
        verify(apps,never()).approvePending(anyLong(),anyLong(),any()); verify(records,never()).insert(any()); verify(logs,never()).append(anyString(),anyLong(),anyString(),any(),any(),anyLong(),any());
    }

    @Test void approveRejectsAnimalNoLongerAvailable(){
        when(apps.selectAnimalIdById(6001L)).thenReturn(5001L); when(animals.selectForUpdate(5001L)).thenReturn(animal(5001L,"ADOPTED")); when(apps.selectForUpdate(6001L)).thenReturn(app(6001L,1001L,5001L,"PENDING"));
        assertEquals(ErrorCode.BUSINESS_STATE_CONFLICT,assertThrows(BusinessException.class,()->service.audit(admin,6001L,auditApprove())).getErrorCode());
    }

    @Test void approveForbidsRejectReasonEvenNullIfFieldWasPresent(){
        AdoptionAuditRequest r=new AdoptionAuditRequest();r.setDecision("APPROVE");r.setRejectReason(null);
        assertEquals(ErrorCode.INVALID_PARAMETER,assertThrows(BusinessException.class,()->service.audit(admin,6001L,r)).getErrorCode());
        verify(apps,never()).selectAnimalIdById(anyLong());
    }

    @Test void rejectRequiresReason(){
        AdoptionAuditRequest r=new AdoptionAuditRequest();r.setDecision("REJECT");
        assertEquals(ErrorCode.INVALID_PARAMETER,assertThrows(BusinessException.class,()->service.audit(admin,6001L,r)).getErrorCode());
    }

    private SubmitAdoptionApplicationRequest request(){SubmitAdoptionApplicationRequest r=new SubmitAdoptionApplicationRequest();r.setAdoptionReason(" reason ");r.setHousingCondition(" home ");r.setFamilyMembers(" family ");r.setPetExperience(" experience ");r.setContact(" 13800138000 ");return r;}
    private AdoptionAuditRequest audit(String d,String reason){AdoptionAuditRequest r=new AdoptionAuditRequest();r.setDecision(d);r.setRejectReason(reason);return r;}
    private AdoptionAuditRequest auditApprove(){AdoptionAuditRequest r=new AdoptionAuditRequest();r.setDecision("APPROVE");return r;}
    private Animal animal(Long id,String status){Animal a=new Animal();a.setId(id);a.setStatus(status);a.setName("Milo");a.setSpecies("CAT");a.setSex("UNKNOWN");return a;}
    private AdoptionApplication app(Long id,Long userId,Long animalId,String status){AdoptionApplication a=new AdoptionApplication();a.setId(id);a.setUserId(userId);a.setAnimalId(animalId);a.setStatus(status);a.setAdoptionReason("r");a.setHousingCondition("h");a.setFamilyMembers("f");a.setPetExperience("p");a.setContact("c");return a;}
    private AdoptionRecord record(Long id,Long appId,Long animalId,Long userId){AdoptionRecord r=new AdoptionRecord();r.setId(id);r.setApplicationId(appId);r.setAnimalId(animalId);r.setUserId(userId);r.setAdoptedAt(LocalDateTime.now());r.setCreatedAt(LocalDateTime.now());return r;}
}


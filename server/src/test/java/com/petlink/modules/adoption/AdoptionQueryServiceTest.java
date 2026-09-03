package com.petlink.modules.adoption;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.modules.adoption.entity.AdoptionApplication;
import com.petlink.modules.adoption.entity.AdoptionRecord;
import com.petlink.modules.adoption.mapper.AdoptionApplicationMapper;
import com.petlink.modules.adoption.mapper.AdoptionRecordMapper;
import com.petlink.modules.adoption.service.AdoptionQueryService;
import com.petlink.modules.adoption.service.AdoptionResponseAssembler;
import com.petlink.modules.animal.entity.Animal;
import com.petlink.modules.animal.mapper.AnimalMapper;
import com.petlink.modules.animal.service.AnimalAccessService;
import com.petlink.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdoptionQueryServiceTest {
    AdoptionApplicationMapper apps; AdoptionRecordMapper records; AnimalMapper animals; AnimalAccessService access; AdoptionResponseAssembler assembler; AdoptionQueryService service;
    @BeforeEach void setUp(){apps=mock(AdoptionApplicationMapper.class);records=mock(AdoptionRecordMapper.class);animals=mock(AnimalMapper.class);access=mock(AnimalAccessService.class);assembler=mock(AdoptionResponseAssembler.class);service=new AdoptionQueryService(apps,records,animals,access,assembler);}

    @Test void myApplicationsAreScopedToCurrentUserAndPaginated(){
        UserPrincipal p=new UserPrincipal(1001L,"USER"); when(apps.selectUserPage(1001L,"PENDING",20,20L)).thenReturn(List.of()); when(apps.countUser(1001L,"PENDING")).thenReturn(21L);
        var out=service.myApplications(p,2,20,"PENDING"); assertEquals(2,out.getPage()); assertEquals(21,out.getTotal()); verify(apps).selectUserPage(1001L,"PENDING",20,20L);
    }
    @Test void invalidApplicationStatusFilterIs400(){
        assertEquals(ErrorCode.INVALID_PARAMETER,assertThrows(BusinessException.class,()->service.myApplications(new UserPrincipal(1L,"USER"),1,20,"BOGUS")).getErrorCode());
    }
    @Test void applicationDetailIs404ForDifferentApplicant(){
        when(apps.selectById(6001L)).thenReturn(app(6001L,2002L,5001L,"PENDING"));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND,assertThrows(BusinessException.class,()->service.applicationDetail(new UserPrincipal(1001L,"USER"),6001L)).getErrorCode());
    }
    @Test void adminCanReadApplicationDetail(){
        AdoptionApplication a=app(6001L,2002L,5001L,"PENDING"); when(apps.selectById(6001L)).thenReturn(a); service.applicationDetail(new UserPrincipal(9L,"ADMIN"),6001L); verify(assembler).detail(eq(a),any());
    }
    @Test void adminQueueDefaultsPendingFifoProjection(){
        when(apps.selectAdminPage("PENDING",20,0)).thenReturn(List.of()); service.adminApplications(new UserPrincipal(9L,"ADMIN"),1,20,null); verify(apps).selectAdminPage("PENDING",20,0L);
    }
    @Test void recordDetailIs404ForNonOwner(){
        AdoptionRecord r=record(7001L,1001L); when(records.selectById(7001L)).thenReturn(r);
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND,assertThrows(BusinessException.class,()->service.recordDetail(new UserPrincipal(1002L,"USER"),7001L)).getErrorCode());
    }
    @Test void ownerCanListHistoricalRecordsEvenAfterAnimalAdopted(){
        when(records.selectUserPage(1001L,20,0)).thenReturn(List.of(record(7001L,1001L))); when(records.countUser(1001L)).thenReturn(1L);
        var out=service.myRecords(new UserPrincipal(1001L,"USER"),1,20); assertEquals(1,out.getTotal()); verify(assembler).record(any());
    }
    @Test void responsibleRescuerGetsReadOnlyOverview(){
        Animal a=new Animal();a.setId(5001L);a.setStatus("ADOPTED"); when(animals.selectById(5001L)).thenReturn(a); UserPrincipal rescuer=new UserPrincipal(2001L,"RESCUER"); when(access.isResponsibleRescuer(rescuer,a)).thenReturn(true);
        when(apps.countPendingByAnimal(5001L)).thenReturn(0L); AdoptionApplication approved=app(6001L,1001L,5001L,"APPROVED");when(apps.selectApprovedByAnimal(5001L)).thenReturn(approved); AdoptionRecord record=record(7001L,1001L);record.setAnimalId(5001L);when(records.selectByAnimalId(5001L)).thenReturn(record);
        var out=service.overview(rescuer,5001L); assertEquals("ADOPTED",out.getAnimalStatus()); assertEquals("6001",out.getApprovedApplicationId()); assertEquals("7001",out.getAdoptionRecordId());
    }
    @Test void nonResponsibleRescuerCannotSeeOverview(){
        Animal a=new Animal();a.setId(5001L);when(animals.selectById(5001L)).thenReturn(a);UserPrincipal r=new UserPrincipal(2001L,"RESCUER");when(access.isResponsibleRescuer(r,a)).thenReturn(false);
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND,assertThrows(BusinessException.class,()->service.overview(r,5001L)).getErrorCode());
    }
    private AdoptionApplication app(Long id,Long user,Long animal,String status){AdoptionApplication a=new AdoptionApplication();a.setId(id);a.setUserId(user);a.setAnimalId(animal);a.setStatus(status);return a;}
    private AdoptionRecord record(Long id,Long user){AdoptionRecord r=new AdoptionRecord();r.setId(id);r.setUserId(user);r.setAnimalId(5001L);r.setApplicationId(6001L);r.setAdoptedAt(LocalDateTime.now());return r;}
}


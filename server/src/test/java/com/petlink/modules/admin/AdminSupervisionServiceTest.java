package com.petlink.modules.admin;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.modules.admin.mapper.AdminMapper;
import com.petlink.modules.admin.service.AdminSupervisionService;
import com.petlink.modules.adoption.service.AdoptionResponseAssembler;
import com.petlink.modules.animal.service.AnimalResponseAssembler;
import com.petlink.modules.rescue.entity.RescueTask;
import com.petlink.modules.rescue.service.RescueTaskResponseAssembler;
import com.petlink.modules.rescue.vo.TaskSummaryResponse;
import com.petlink.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminSupervisionServiceTest {
    AdminMapper mapper;RescueTaskResponseAssembler tasks;AnimalResponseAssembler animals;AdoptionResponseAssembler records;AdminSupervisionService service;
    UserPrincipal admin=new UserPrincipal(1L,"ADMIN");
    @BeforeEach void setUp(){mapper=mock(AdminMapper.class);tasks=mock(RescueTaskResponseAssembler.class);animals=mock(AnimalResponseAssembler.class);records=mock(AdoptionResponseAssembler.class);service=new AdminSupervisionService(mapper,tasks,animals,records);}
    @Test void taskFiltersPagingAndFrozenAssembler(){RescueTask task=new RescueTask();TaskSummaryResponse dto=mock(TaskSummaryResponse.class);when(mapper.selectTasksPage("IN_PROGRESS",2L,3L,20,20L)).thenReturn(List.of(task));when(mapper.countTasks("IN_PROGRESS",2L,3L)).thenReturn(21L);when(tasks.summary(task)).thenReturn(dto);var page=service.tasks(admin,2,20,"IN_PROGRESS",2L,3L);assertEquals(21,page.getTotal());assertSame(dto,page.getRecords().get(0));}
    @Test void taskUnknownStatusRejected(){assertEquals(ErrorCode.INVALID_PARAMETER,error(()->service.tasks(admin,1,20,"OPEN",null,null)));}
    @Test void taskInvalidFilterIdRejected(){assertEquals(ErrorCode.INVALID_PARAMETER,error(()->service.tasks(admin,1,20,null,0L,null)));}
    @Test void animalFiltersUseFrozenAssembler(){service.animals(admin,1,20,"AVAILABLE"," CAT ",9L);verify(mapper).selectAnimalsPage("AVAILABLE","CAT",9L,20,0L);verify(mapper).countAnimals("AVAILABLE","CAT",9L);}
    @Test void animalUnknownStatusRejected(){assertEquals(ErrorCode.INVALID_PARAMETER,error(()->service.animals(admin,1,20,"LOST",null,null)));}
    @Test void speciesOver50Rejected(){assertEquals(ErrorCode.INVALID_PARAMETER,error(()->service.animals(admin,1,20,null,"x".repeat(51),null)));}
    @Test void recordsPassAllFrozenFilters(){service.adoptionRecords(admin,3,10,7L,8L,9L);verify(mapper).selectAdoptionRecordsPage(7L,8L,9L,10,20L);verify(mapper).countAdoptionRecords(7L,8L,9L);}
    @Test void recordsInvalidIdRejected(){assertEquals(ErrorCode.INVALID_PARAMETER,error(()->service.adoptionRecords(admin,1,20,-1L,null,null)));}
    @Test void nonAdminCannotSupervise(){assertEquals(ErrorCode.FORBIDDEN,error(()->service.tasks(new UserPrincipal(2L,"RESCUER"),1,20,null,null,null)));}
    private ErrorCode error(Runnable action){return assertThrows(BusinessException.class,action::run).getErrorCode();}
}

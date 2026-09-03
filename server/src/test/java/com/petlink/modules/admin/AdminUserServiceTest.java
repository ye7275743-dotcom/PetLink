package com.petlink.modules.admin;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.infrastructure.audit.service.OperationLogService;
import com.petlink.modules.admin.mapper.AdminMapper;
import com.petlink.modules.admin.service.AdminResponseAssembler;
import com.petlink.modules.admin.service.AdminUserService;
import com.petlink.modules.auth.entity.SysUser;
import com.petlink.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminUserServiceTest {
    AdminMapper mapper;OperationLogService logs;AdminUserService service;
    UserPrincipal admin=new UserPrincipal(1L,"ADMIN");

    @BeforeEach void setUp(){mapper=mock(AdminMapper.class);logs=mock(OperationLogService.class);service=new AdminUserService(mapper,new AdminResponseAssembler(),logs);}

    @Test void listNormalizesFiltersAndNeverExposesPassword(){SysUser u=user(8L,"USER","ENABLED");u.setPasswordHash("secret");when(mapper.selectUsersPage("USER","ENABLED","alex",20,20L)).thenReturn(List.of(u));when(mapper.countUsers("USER","ENABLED","alex")).thenReturn(21L);var page=service.list(admin,2,20,"USER","ENABLED"," alex ");assertEquals(21,page.getTotal());assertEquals("8",page.getRecords().get(0).getId());assertFalse(page.getRecords().get(0).getClass().getDeclaredFields().toString().contains("passwordHash"));}
    @Test void listRejectsUnknownRole(){assertEquals(ErrorCode.INVALID_PARAMETER,error(()->service.list(admin,1,20,"OWNER",null,null)));}
    @Test void listRejectsUnknownStatus(){assertEquals(ErrorCode.INVALID_PARAMETER,error(()->service.list(admin,1,20,null,"LOCKED",null)));}
    @Test void listRejectsBadPaging(){assertEquals(ErrorCode.INVALID_PARAMETER,error(()->service.list(admin,0,20,null,null,null)));}
    @Test void detailAlwaysContainsFourStatistics(){SysUser u=user(8L,"USER","ENABLED");when(mapper.selectUser(8L)).thenReturn(u);when(mapper.countPublishedClues(8L)).thenReturn(3L);when(mapper.countRescueTasks(8L)).thenReturn(4L);when(mapper.countAdoptionApplications(8L)).thenReturn(5L);when(mapper.countAdoptionRecordsByUser(8L)).thenReturn(6L);var d=service.detail(admin,8L);assertEquals(3,d.getStatistics().getPublishedClueCount());assertEquals(4,d.getStatistics().getRescueTaskCount());assertEquals(5,d.getStatistics().getAdoptionApplicationCount());assertEquals(6,d.getStatistics().getAdoptionRecordCount());}
    @Test void detailMissingReturns404(){assertEquals(ErrorCode.RESOURCE_NOT_FOUND,error(()->service.detail(admin,99L)));}
    @Test void enableUsesConditionalUpdateAndLogs(){SysUser enabled=user(8L,"USER","ENABLED");when(mapper.enableUser(eq(8L),any())).thenReturn(1);when(mapper.selectUser(8L)).thenReturn(enabled);var out=service.enable(admin,8L);assertEquals("ENABLED",out.getStatus());verify(logs).append("SYS_USER",8L,"ENABLE","DISABLED","ENABLED",1L,null);}
    @Test void enableAlreadyEnabledIsConflict(){when(mapper.selectUser(8L)).thenReturn(user(8L,"USER","ENABLED"));assertEquals(ErrorCode.BUSINESS_STATE_CONFLICT,error(()->service.enable(admin,8L)));verifyNoInteractions(logs);}
    @Test void disableUsesConditionalUpdateAndLogs(){SysUser disabled=user(8L,"RESCUER","DISABLED");when(mapper.disableUser(eq(8L),any())).thenReturn(1);when(mapper.selectUser(8L)).thenReturn(disabled);service.disable(admin,8L);verify(logs).append("SYS_USER",8L,"DISABLE","ENABLED","DISABLED",1L,null);}
    @Test void adminCannotDisableSelf(){assertEquals(ErrorCode.FORBIDDEN,error(()->service.disable(admin,1L)));verifyNoInteractions(mapper,logs);}
    @Test void disableMissingReturns404(){assertEquals(ErrorCode.RESOURCE_NOT_FOUND,error(()->service.disable(admin,99L)));}
    @Test void promoteUserAndLogRoleOperation(){SysUser rescuer=user(8L,"RESCUER","ENABLED");when(mapper.promoteRescuer(eq(8L),any())).thenReturn(1);when(mapper.selectUser(8L)).thenReturn(rescuer);var out=service.promoteRescuer(admin,8L);assertEquals("RESCUER",out.getRoleCode());verify(logs).append("SYS_USER",8L,"PROMOTE_RESCUER",null,null,1L,null);}
    @Test void promoteRescuerAgainIsConflict(){when(mapper.selectUser(8L)).thenReturn(user(8L,"RESCUER","ENABLED"));assertEquals(ErrorCode.BUSINESS_STATE_CONFLICT,error(()->service.promoteRescuer(admin,8L)));}
    @Test void nonAdminCannotUseService(){assertEquals(ErrorCode.FORBIDDEN,error(()->service.list(new UserPrincipal(2L,"USER"),1,20,null,null,null)));}

    private SysUser user(long id,String role,String status){SysUser u=new SysUser();u.setId(id);u.setAccount("acct"+id);u.setNickname("User");u.setRoleCode(role);u.setStatus(status);u.setCreatedAt(LocalDateTime.now());u.setUpdatedAt(LocalDateTime.now());return u;}
    private ErrorCode error(Runnable action){return assertThrows(BusinessException.class,action::run).getErrorCode();}
}

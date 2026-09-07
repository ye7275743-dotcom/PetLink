package com.petlink.modules.admin;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.infrastructure.audit.service.OperationLogService;
import com.petlink.modules.admin.dto.ChangeRoleRequest;
import com.petlink.modules.admin.mapper.AdminMapper;
import com.petlink.modules.admin.service.AdminResponseAssembler;
import com.petlink.modules.admin.service.AdminUserService;
import com.petlink.modules.auth.entity.SysUser;
import com.petlink.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AdminRoleChangeTest {
    final AdminMapper mapper=mock(AdminMapper.class);
    final OperationLogService logs=mock(OperationLogService.class);
    final AdminUserService service=new AdminUserService(mapper,new AdminResponseAssembler(),logs);
    final UserPrincipal admin=new UserPrincipal(1L,"ADMIN");
    ChangeRoleRequest body(String role,String reason){var b=new ChangeRoleRequest();b.setRoleCode(role);b.setReason(reason);return b;}
    void user(String role){var u=new SysUser();u.setId(2L);u.setRoleCode(role);u.setStatus("ENABLED");when(mapper.lockUser(2L)).thenReturn(u);when(mapper.selectUser(2L)).thenReturn(u);}
    @Test void demotionBlockedByActiveTask(){user("RESCUER");when(mapper.countActiveTasks(2L)).thenReturn(1L);assertEquals(ErrorCode.BUSINESS_STATE_CONFLICT,assertThrows(BusinessException.class,()->service.changeRole(admin,2L,body("USER","结束值班"))).getErrorCode());verify(mapper,never()).changeRole(any(),any(),any(),any());verifyNoInteractions(logs);}
    @Test void demotionRecordsReasonAndRoles(){user("RESCUER");when(mapper.changeRole(eq(2L),eq("RESCUER"),eq("USER"),any())).thenReturn(1);service.changeRole(admin,2L,body("USER","  结束值班  "));verify(logs).append("SYS_USER",2L,"DEMOTE_RESCUER","RESCUER","USER",1L,"结束值班");var order=inOrder(mapper);order.verify(mapper).lockUser(2L);order.verify(mapper).countActiveTasks(2L);order.verify(mapper).changeRole(eq(2L),eq("RESCUER"),eq("USER"),any());}
    @Test void promotionRecordsReason(){user("USER");when(mapper.changeRole(eq(2L),eq("USER"),eq("RESCUER"),any())).thenReturn(1);service.changeRole(admin,2L,body("RESCUER","审核通过"));verify(logs).append("SYS_USER",2L,"PROMOTE_RESCUER","USER","RESCUER",1L,"审核通过");}
    @Test void adminCannotBeDemoted(){user("ADMIN");assertEquals(ErrorCode.FORBIDDEN,assertThrows(BusinessException.class,()->service.changeRole(admin,2L,body("USER","测试"))).getErrorCode());}
    @Test void cannotAppointAdmin(){assertThrows(BusinessException.class,()->service.changeRole(admin,2L,body("ADMIN","测试")));verifyNoInteractions(mapper);}
    @Test void reasonRequired(){assertThrows(BusinessException.class,()->service.changeRole(admin,2L,body("USER"," ")));verifyNoInteractions(mapper);}
    @Test void userCannotManageRoles(){assertEquals(ErrorCode.FORBIDDEN,assertThrows(BusinessException.class,()->service.changeRole(new UserPrincipal(3L,"USER"),2L,body("USER","测试"))).getErrorCode());}
    @Test void sameRoleConflicts(){user("USER");assertThrows(BusinessException.class,()->service.changeRole(admin,2L,body("USER","测试")));}
}

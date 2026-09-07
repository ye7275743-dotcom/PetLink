package com.petlink.modules.admin;
import com.petlink.common.*;
import com.petlink.infrastructure.audit.service.OperationLogService;
import com.petlink.modules.admin.mapper.AdminMapper;
import com.petlink.modules.admin.service.*;
import com.petlink.modules.auth.entity.SysUser;
import com.petlink.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class UserDeletionTest {
 final AdminMapper mapper=mock(AdminMapper.class);final OperationLogService logs=mock(OperationLogService.class);
 final AdminUserService service=new AdminUserService(mapper,new AdminResponseAssembler(),logs);
 final UserPrincipal admin=new UserPrincipal(1L,"ADMIN");
 void user(String role){SysUser u=new SysUser();u.setId(2L);u.setRoleCode(role);u.setStatus("ENABLED");when(mapper.lockUser(2L)).thenReturn(u);}
 @Test void nonAdminCannotDelete(){assertThrows(BusinessException.class,()->service.deleteUser(new UserPrincipal(3L,"USER"),2L));verifyNoInteractions(mapper);}
 @Test void cannotDeleteAdmin(){user("ADMIN");assertEquals(ErrorCode.FORBIDDEN,assertThrows(BusinessException.class,()->service.deleteUser(admin,2L)).getErrorCode());verify(mapper,never()).softDeleteUser(any(),any());}
 @Test void cannotDeleteMissing(){assertEquals(ErrorCode.RESOURCE_NOT_FOUND,assertThrows(BusinessException.class,()->service.deleteUser(admin,2L)).getErrorCode());}
 @Test void cannotDeleteActiveRescuer(){user("RESCUER");when(mapper.countActiveTasks(2L)).thenReturn(1L);assertEquals(ErrorCode.BUSINESS_STATE_CONFLICT,assertThrows(BusinessException.class,()->service.deleteUser(admin,2L)).getErrorCode());verifyNoInteractions(logs);}
 @Test void deletionLocksAccountAndAudits(){user("USER");when(mapper.softDeleteUser(eq(2L),any())).thenReturn(1);service.deleteUser(admin,2L);var order=inOrder(mapper,logs);order.verify(mapper).lockUser(2L);order.verify(mapper).countActiveTasks(2L);order.verify(mapper).softDeleteUser(eq(2L),any());order.verify(logs).append(eq("SYS_USER"),eq(2L),eq("DELETE_USER"),eq("ENABLED"),eq("DELETED"),eq(1L),anyString());}
 @Test void failedUpdateDoesNotAudit(){user("USER");assertThrows(BusinessException.class,()->service.deleteUser(admin,2L));verifyNoInteractions(logs);}
}

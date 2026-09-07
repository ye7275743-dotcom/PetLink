package com.petlink.modules.content;
import com.petlink.common.*;
import com.petlink.infrastructure.audit.service.OperationLogService;
import com.petlink.modules.content.entity.Announcement;
import com.petlink.modules.content.mapper.AnnouncementMapper;
import com.petlink.modules.content.service.*;
import com.petlink.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class AnnouncementDeletionTest {
 final AnnouncementMapper mapper=mock(AnnouncementMapper.class);final OperationLogService logs=mock(OperationLogService.class);
 final AnnouncementTransactionalService service=new AnnouncementTransactionalService(mapper,logs,new AnnouncementResponseAssembler());
 final UserPrincipal admin=new UserPrincipal(1L,"ADMIN");
 void announcement(){var a=new Announcement();a.setId(2L);a.setVersion(3);a.setStatus("PUBLISHED");when(mapper.lockActive(2L)).thenReturn(a);}
 @Test void userForbidden(){assertThrows(BusinessException.class,()->service.delete(new UserPrincipal(2L,"USER"),2L,3));verifyNoInteractions(mapper);}
 @Test void staleVersionConflicts(){announcement();assertEquals(ErrorCode.OPTIMISTIC_LOCK_CONFLICT,assertThrows(BusinessException.class,()->service.delete(admin,2L,2)).getErrorCode());verifyNoInteractions(logs);}
 @Test void missingIs404(){assertEquals(ErrorCode.RESOURCE_NOT_FOUND,assertThrows(BusinessException.class,()->service.delete(admin,2L,3)).getErrorCode());}
 @Test void publishedDeletionAudits(){announcement();when(mapper.softDelete(eq(2L),eq(3),eq(1L),any())).thenReturn(1);service.delete(admin,2L,3);verify(logs).append(eq("ANNOUNCEMENT"),eq(2L),eq("DELETE_ANNOUNCEMENT"),eq("PUBLISHED"),eq("DELETED"),eq(1L),anyString());}
}

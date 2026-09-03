package com.petlink.modules.content;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.modules.content.entity.Announcement;
import com.petlink.modules.content.mapper.AnnouncementMapper;
import com.petlink.modules.content.service.AnnouncementQueryService;
import com.petlink.modules.content.service.AnnouncementResponseAssembler;
import com.petlink.modules.content.vo.AnnouncementAdminSummaryResponse;
import com.petlink.modules.content.vo.AnnouncementPublicSummaryResponse;
import com.petlink.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AnnouncementQueryServiceTest {
    AnnouncementMapper mapper;AnnouncementResponseAssembler assembler;AnnouncementQueryService service;UserPrincipal admin=new UserPrincipal(1L,"ADMIN");
    @BeforeEach void setUp(){mapper=mock(AnnouncementMapper.class);assembler=mock(AnnouncementResponseAssembler.class);service=new AnnouncementQueryService(mapper,assembler);}
    @Test void publicListUsesPublishedMapperAndPaging(){Announcement a=new Announcement();when(mapper.selectPublicPage(20,20L)).thenReturn(List.of(a));when(mapper.countPublic()).thenReturn(21L);when(assembler.publicSummary(a)).thenReturn(mock(AnnouncementPublicSummaryResponse.class));var p=service.publicList(2,20);assertEquals(21,p.getTotal());assertEquals(1,p.getRecords().size());}
    @Test void publicDetailHidesDraftWithdrawnAndMissingAs404(){when(mapper.selectPublishedById(5L)).thenReturn(null);assertEquals(ErrorCode.RESOURCE_NOT_FOUND,error(()->service.publicDetail(5L)));}
    @Test void publicPageSizeOver100Rejected(){assertEquals(ErrorCode.INVALID_PARAMETER,error(()->service.publicList(1,101)));}
    @Test void adminListAllowsFrozenStatus(){Announcement a=new Announcement();when(mapper.selectAdminPage("DRAFT",20,0L)).thenReturn(List.of(a));when(assembler.adminSummary(a)).thenReturn(mock(AnnouncementAdminSummaryResponse.class));assertEquals(1,service.adminList(admin,1,20,"DRAFT").getRecords().size());}
    @Test void adminListRejectsUnknownStatus(){assertEquals(ErrorCode.INVALID_PARAMETER,error(()->service.adminList(admin,1,20,"ARCHIVED")));}
    @Test void nonAdminCannotUseAdminQueries(){assertEquals(ErrorCode.FORBIDDEN,error(()->service.adminList(new UserPrincipal(2L,"USER"),1,20,null)));}
    @Test void adminDetailReturnsAllStatuses(){Announcement a=new Announcement();a.setId(5L);when(mapper.selectById(5L)).thenReturn(a);service.adminDetail(admin,5L);verify(assembler).adminDetail(a);}
    private ErrorCode error(Runnable r){return assertThrows(BusinessException.class,r::run).getErrorCode();}
}

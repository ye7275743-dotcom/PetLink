package com.petlink.modules.content;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.infrastructure.audit.service.OperationLogService;
import com.petlink.modules.content.dto.AnnouncementVersionRequest;
import com.petlink.modules.content.dto.CreateAnnouncementRequest;
import com.petlink.modules.content.dto.PatchAnnouncementRequest;
import com.petlink.modules.content.entity.Announcement;
import com.petlink.modules.content.mapper.AnnouncementMapper;
import com.petlink.modules.content.service.AnnouncementResponseAssembler;
import com.petlink.modules.content.service.AnnouncementTransactionalService;
import com.petlink.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AnnouncementTransactionalServiceTest {
    AnnouncementMapper mapper;OperationLogService logs;AnnouncementResponseAssembler assembler;AnnouncementTransactionalService service;UserPrincipal admin=new UserPrincipal(1L,"ADMIN");
    @BeforeEach void setUp(){mapper=mock(AnnouncementMapper.class);logs=mock(OperationLogService.class);assembler=mock(AnnouncementResponseAssembler.class);service=new AnnouncementTransactionalService(mapper,logs,assembler);}
    @Test void createTrimsDraftAndLogsInSameServiceCall(){when(mapper.insert(any())).thenAnswer(i->{Announcement a=i.getArgument(0);a.setId(5L);return 1;});when(mapper.selectById(5L)).thenReturn(announcement("DRAFT",0));service.create(admin,create(" title "," content "));verify(mapper).insert(argThat(a->"title".equals(a.getTitle())&&"content".equals(a.getContent())&&"DRAFT".equals(a.getStatus())&&a.getCreatedBy().equals(1L)&&a.getUpdatedBy().equals(1L)&&a.getVersion()==0));verify(logs).append("ANNOUNCEMENT",5L,"CREATE",null,"DRAFT",1L,null);}
    @Test void createRejectsBlankTitle(){assertEquals(ErrorCode.INVALID_PARAMETER,error(()->service.create(admin,create(" ","content"))));}
    @Test void createRejectsBlankContent(){assertEquals(ErrorCode.INVALID_PARAMETER,error(()->service.create(admin,create("title"," "))));}
    @Test void patchTitleOnlyUsesPresenceAndVersion(){when(mapper.updateContent(eq(5L),eq("new"),eq(true),isNull(),eq(false),eq(1L),eq(2),any())).thenReturn(1);when(mapper.selectById(5L)).thenReturn(announcement("PUBLISHED",3));service.patch(admin,5L,patchTitle(" new ",2));verifyNoInteractions(logs);}
    @Test void patchOnlyVersionIsInvalid(){PatchAnnouncementRequest r=new PatchAnnouncementRequest();r.setVersion(2);assertEquals(ErrorCode.INVALID_PARAMETER,error(()->service.patch(admin,5L,r)));}
    @Test void patchMissingVersionIsInvalid(){PatchAnnouncementRequest r=new PatchAnnouncementRequest();r.setTitle("x");assertEquals(ErrorCode.INVALID_PARAMETER,error(()->service.patch(admin,5L,r)));}
    @Test void patchWithdrawnIsStateConflict(){when(mapper.updateContent(anyLong(),any(),anyBoolean(),any(),anyBoolean(),anyLong(),anyInt(),any())).thenReturn(0);when(mapper.selectById(5L)).thenReturn(announcement("WITHDRAWN",2));assertEquals(ErrorCode.BUSINESS_STATE_CONFLICT,error(()->service.patch(admin,5L,patchTitle("x",2))));}
    @Test void patchStaleVersionIs40903(){when(mapper.updateContent(anyLong(),any(),anyBoolean(),any(),anyBoolean(),anyLong(),anyInt(),any())).thenReturn(0);when(mapper.selectById(5L)).thenReturn(announcement("DRAFT",3));assertEquals(ErrorCode.OPTIMISTIC_LOCK_CONFLICT,error(()->service.patch(admin,5L,patchTitle("x",2))));}
    @Test void publishDraftSetsStateAndLogs(){when(mapper.publish(eq(5L),eq(1L),eq(0),any())).thenReturn(1);when(mapper.selectById(5L)).thenReturn(announcement("PUBLISHED",1));service.publish(admin,5L,version(0));verify(logs).append("ANNOUNCEMENT",5L,"PUBLISH","DRAFT","PUBLISHED",1L,null);}
    @Test void publishWrongStateIs40901(){when(mapper.selectById(5L)).thenReturn(announcement("PUBLISHED",1));assertEquals(ErrorCode.BUSINESS_STATE_CONFLICT,error(()->service.publish(admin,5L,version(1))));}
    @Test void publishStaleVersionIs40903(){when(mapper.selectById(5L)).thenReturn(announcement("DRAFT",2));assertEquals(ErrorCode.OPTIMISTIC_LOCK_CONFLICT,error(()->service.publish(admin,5L,version(1))));}
    @Test void withdrawPublishedLogsAndKeepsLifecycle(){when(mapper.withdraw(eq(5L),eq(1L),eq(1),any())).thenReturn(1);when(mapper.selectById(5L)).thenReturn(announcement("WITHDRAWN",2));service.withdraw(admin,5L,version(1));verify(logs).append("ANNOUNCEMENT",5L,"WITHDRAW","PUBLISHED","WITHDRAWN",1L,null);}
    @Test void withdrawDraftIs40901(){when(mapper.selectById(5L)).thenReturn(announcement("DRAFT",0));assertEquals(ErrorCode.BUSINESS_STATE_CONFLICT,error(()->service.withdraw(admin,5L,version(0))));}
    @Test void withdrawStaleVersionIs40903(){when(mapper.selectById(5L)).thenReturn(announcement("PUBLISHED",2));assertEquals(ErrorCode.OPTIMISTIC_LOCK_CONFLICT,error(()->service.withdraw(admin,5L,version(1))));}
    @Test void negativeVersionRejectedBeforeMapper(){assertEquals(ErrorCode.INVALID_PARAMETER,error(()->service.publish(admin,5L,version(-1))));verifyNoInteractions(mapper);}
    @Test void nonAdminCannotWrite(){assertEquals(ErrorCode.FORBIDDEN,error(()->service.create(new UserPrincipal(2L,"USER"),create("t","c"))));}
    private CreateAnnouncementRequest create(String t,String c){CreateAnnouncementRequest r=new CreateAnnouncementRequest();r.setTitle(t);r.setContent(c);return r;}
    private PatchAnnouncementRequest patchTitle(String t,int v){PatchAnnouncementRequest r=new PatchAnnouncementRequest();r.setTitle(t);r.setVersion(v);return r;}
    private AnnouncementVersionRequest version(int v){AnnouncementVersionRequest r=new AnnouncementVersionRequest();r.setVersion(v);return r;}
    private Announcement announcement(String s,int v){Announcement a=new Announcement();a.setId(5L);a.setTitle("t");a.setContent("c");a.setStatus(s);a.setCreatedBy(1L);a.setUpdatedBy(1L);a.setVersion(v);return a;}
    private ErrorCode error(Runnable r){return assertThrows(BusinessException.class,r::run).getErrorCode();}
}

package com.petlink.modules.content.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.common.TimeUtils;
import com.petlink.infrastructure.audit.service.OperationLogService;
import com.petlink.modules.content.dto.AnnouncementVersionRequest;
import com.petlink.modules.content.dto.CreateAnnouncementRequest;
import com.petlink.modules.content.dto.PatchAnnouncementRequest;
import com.petlink.modules.content.entity.Announcement;
import com.petlink.modules.content.mapper.AnnouncementMapper;
import com.petlink.modules.content.vo.AnnouncementAdminDetailResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AnnouncementTransactionalService {
    private final AnnouncementMapper mapper;private final OperationLogService logs;private final AnnouncementResponseAssembler assembler;
    public AnnouncementTransactionalService(AnnouncementMapper mapper,OperationLogService logs,AnnouncementResponseAssembler assembler){this.mapper=mapper;this.logs=logs;this.assembler=assembler;}

    @Transactional
    public AnnouncementAdminDetailResponse create(UserPrincipal admin,CreateAnnouncementRequest request){
        AnnouncementQueryService.requireAdmin(admin);if(request==null)throw invalid();
        Announcement a=new Announcement();a.setTitle(requiredText(request.getTitle(),200));a.setContent(requiredText(request.getContent(),Integer.MAX_VALUE));a.setStatus("DRAFT");a.setCreatedBy(admin.getUserId());a.setUpdatedBy(admin.getUserId());a.setVersion(0);
        if(mapper.insert(a)!=1)throw new IllegalStateException("announcement insert affected rows != 1");
        logs.append("ANNOUNCEMENT",a.getId(),"CREATE",null,"DRAFT",admin.getUserId(),null);
        return assembler.adminDetail(required(a.getId()));
    }

    @Transactional
    public AnnouncementAdminDetailResponse patch(UserPrincipal admin,Long id,PatchAnnouncementRequest request){
        AnnouncementQueryService.requireAdmin(admin);AnnouncementQueryService.requireId(id);
        if(request==null||request.getVersion()==null||request.getVersion()<0||(!request.isTitlePresent()&&!request.isContentPresent()))throw invalid();
        String title=request.isTitlePresent()?requiredText(request.getTitle(),200):null;
        String content=request.isContentPresent()?requiredText(request.getContent(),Integer.MAX_VALUE):null;
        LocalDateTime now=LocalDateTime.now(TimeUtils.ZONE);
        if(mapper.updateContent(id,title,request.isTitlePresent(),content,request.isContentPresent(),admin.getUserId(),request.getVersion(),now)!=1)resolveFailure(id,"DRAFT_OR_PUBLISHED",request.getVersion());
        return assembler.adminDetail(required(id));
    }

    @Transactional
    public AnnouncementAdminDetailResponse publish(UserPrincipal admin,Long id,AnnouncementVersionRequest request){
        AnnouncementQueryService.requireAdmin(admin);Integer version=version(request);LocalDateTime now=LocalDateTime.now(TimeUtils.ZONE);
        if(mapper.publish(id,admin.getUserId(),version,now)!=1)resolveFailure(id,"DRAFT",version);
        logs.append("ANNOUNCEMENT",id,"PUBLISH","DRAFT","PUBLISHED",admin.getUserId(),null);
        return assembler.adminDetail(required(id));
    }

    @Transactional
    public AnnouncementAdminDetailResponse withdraw(UserPrincipal admin,Long id,AnnouncementVersionRequest request){
        AnnouncementQueryService.requireAdmin(admin);Integer version=version(request);LocalDateTime now=LocalDateTime.now(TimeUtils.ZONE);
        if(mapper.withdraw(id,admin.getUserId(),version,now)!=1)resolveFailure(id,"PUBLISHED",version);
        logs.append("ANNOUNCEMENT",id,"WITHDRAW","PUBLISHED","WITHDRAWN",admin.getUserId(),null);
        return assembler.adminDetail(required(id));
    }

    private Integer version(AnnouncementVersionRequest request){if(request==null||request.getVersion()==null||request.getVersion()<0)throw invalid();return request.getVersion();}
    private void resolveFailure(Long id,String expected,Integer version){Announcement a=mapper.selectById(id);if(a==null)throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);boolean stateOk="DRAFT_OR_PUBLISHED".equals(expected)?("DRAFT".equals(a.getStatus())||"PUBLISHED".equals(a.getStatus())):expected.equals(a.getStatus());if(!stateOk)throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);if(!version.equals(a.getVersion()))throw new BusinessException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT);throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);}
    private Announcement required(Long id){Announcement a=mapper.selectById(id);if(a==null)throw new IllegalStateException("announcement disappeared");return a;}
    private String requiredText(String text,int max){if(text==null)throw invalid();String v=text.trim();if(v.isEmpty()||v.length()>max)throw invalid();return v;}
    private BusinessException invalid(){return new BusinessException(ErrorCode.INVALID_PARAMETER);}
}

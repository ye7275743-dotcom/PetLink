package com.petlink.modules.adoption.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.common.PageResponse;
import com.petlink.common.TimeUtils;
import com.petlink.modules.adoption.entity.AdoptionApplication;
import com.petlink.modules.adoption.entity.AdoptionRecord;
import com.petlink.modules.adoption.mapper.AdoptionApplicationMapper;
import com.petlink.modules.adoption.mapper.AdoptionRecordMapper;
import com.petlink.modules.adoption.vo.AdoptionApplicationDetailResponse;
import com.petlink.modules.adoption.vo.AdoptionApplicationSummaryResponse;
import com.petlink.modules.adoption.vo.AdoptionOverviewResponse;
import com.petlink.modules.adoption.vo.AdoptionRecordResponse;
import com.petlink.modules.animal.entity.Animal;
import com.petlink.modules.animal.mapper.AnimalMapper;
import com.petlink.modules.animal.service.AnimalAccessService;
import com.petlink.security.UserPrincipal;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdoptionQueryService {
    private static final Set<String> STATUSES=Set.of("PENDING","APPROVED","REJECTED","WITHDRAWN","INVALIDATED");
    private final AdoptionApplicationMapper applicationMapper;
    private final AdoptionRecordMapper recordMapper;
    private final AnimalMapper animalMapper;
    private final AnimalAccessService animalAccess;
    private final AdoptionResponseAssembler assembler;

    public AdoptionQueryService(AdoptionApplicationMapper applicationMapper,AdoptionRecordMapper recordMapper,AnimalMapper animalMapper,
                                AnimalAccessService animalAccess,AdoptionResponseAssembler assembler){
        this.applicationMapper=applicationMapper;this.recordMapper=recordMapper;this.animalMapper=animalMapper;this.animalAccess=animalAccess;this.assembler=assembler;
    }

    public PageResponse<AdoptionApplicationSummaryResponse> myApplications(UserPrincipal principal,int page,int size,String status){
        requireApplicantRole(principal); validatePage(page,size); String normalized=normalizeOptionalStatus(status);
        long offset=(long)(page-1)*size;
        List<AdoptionApplicationSummaryResponse> rows=applicationMapper.selectUserPage(principal.getUserId(),normalized,size,offset).stream().map(assembler::summary).collect(Collectors.toList());
        return new PageResponse<>(rows,page,size,applicationMapper.countUser(principal.getUserId(),normalized));
    }

    public AdoptionApplicationDetailResponse applicationDetail(UserPrincipal principal,Long applicationId){
        requireId(applicationId); if(principal==null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        AdoptionApplication app=applicationMapper.selectById(applicationId);
        if(app==null || (!"ADMIN".equals(principal.getRoleCode()) && !principal.getUserId().equals(app.getUserId()))) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return assembler.detail(app,principal);
    }

    public PageResponse<AdoptionApplicationSummaryResponse> adminApplications(UserPrincipal principal,int page,int size,String status){
        requireAdmin(principal); validatePage(page,size); String normalized=status==null||status.isBlank()?"PENDING":normalizeStatus(status);
        long offset=(long)(page-1)*size;
        List<AdoptionApplicationSummaryResponse> rows=applicationMapper.selectAdminPage(normalized,size,offset).stream().map(assembler::summary).collect(Collectors.toList());
        return new PageResponse<>(rows,page,size,applicationMapper.countAdmin(normalized));
    }

    public PageResponse<AdoptionRecordResponse> myRecords(UserPrincipal principal,int page,int size){
        requireApplicantRole(principal); validatePage(page,size); long offset=(long)(page-1)*size;
        List<AdoptionRecordResponse> rows=recordMapper.selectUserPage(principal.getUserId(),size,offset).stream().map(assembler::record).collect(Collectors.toList());
        return new PageResponse<>(rows,page,size,recordMapper.countUser(principal.getUserId()));
    }

    public AdoptionRecordResponse recordDetail(UserPrincipal principal,Long recordId){
        requireId(recordId); if(principal==null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        AdoptionRecord record=recordMapper.selectById(recordId);
        if(record==null || (!"ADMIN".equals(principal.getRoleCode()) && !principal.getUserId().equals(record.getUserId()))) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return assembler.record(record);
    }

    public AdoptionOverviewResponse overview(UserPrincipal principal,Long animalId){
        if(principal==null || !"RESCUER".equals(principal.getRoleCode())) throw new BusinessException(ErrorCode.FORBIDDEN);
        requireId(animalId); Animal animal=animalMapper.selectById(animalId);
        if(animal==null || !animalAccess.isResponsibleRescuer(principal,animal)) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        long pending=applicationMapper.countPendingByAnimal(animalId);
        AdoptionApplication approved=applicationMapper.selectApprovedByAnimal(animalId);
        AdoptionRecord record=recordMapper.selectByAnimalId(animalId);
        return new AdoptionOverviewResponse(String.valueOf(animalId),animal.getStatus(),pending,
                approved==null?null:String.valueOf(approved.getId()),record==null?null:String.valueOf(record.getId()),
                record==null?null:TimeUtils.toOffset(record.getAdoptedAt()));
    }

    private String normalizeOptionalStatus(String status){return status==null||status.isBlank()?null:normalizeStatus(status);}
    private String normalizeStatus(String status){String v=status.trim(); if(!STATUSES.contains(v)) throw new BusinessException(ErrorCode.INVALID_PARAMETER); return v;}
    private void validatePage(int page,int size){if(page<1||size<1||size>100) throw new BusinessException(ErrorCode.INVALID_PARAMETER);}
    private void requireApplicantRole(UserPrincipal p){if(p==null || !("USER".equals(p.getRoleCode())||"RESCUER".equals(p.getRoleCode()))) throw new BusinessException(ErrorCode.FORBIDDEN);}
    private void requireAdmin(UserPrincipal p){if(p==null||!"ADMIN".equals(p.getRoleCode())) throw new BusinessException(ErrorCode.FORBIDDEN);}
    private void requireId(Long id){if(id==null||id<=0) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);}
}


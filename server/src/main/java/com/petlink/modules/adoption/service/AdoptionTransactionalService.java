package com.petlink.modules.adoption.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.common.TimeUtils;
import com.petlink.infrastructure.audit.service.OperationLogService;
import com.petlink.modules.adoption.dto.AdoptionAuditRequest;
import com.petlink.modules.adoption.dto.SubmitAdoptionApplicationRequest;
import com.petlink.modules.adoption.entity.AdoptionApplication;
import com.petlink.modules.adoption.entity.AdoptionRecord;
import com.petlink.modules.adoption.mapper.AdoptionApplicationMapper;
import com.petlink.modules.adoption.mapper.AdoptionRecordMapper;
import com.petlink.modules.adoption.vo.AdoptionApplicationDetailResponse;
import com.petlink.modules.adoption.vo.AdoptionAuditResponse;
import com.petlink.modules.adoption.vo.AdoptionRecordResponse;
import com.petlink.modules.adoption.vo.AdoptionStateActionResponse;
import com.petlink.modules.animal.entity.Animal;
import com.petlink.modules.animal.mapper.AnimalMapper;
import com.petlink.security.UserPrincipal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class AdoptionTransactionalService {
    private final AdoptionApplicationMapper applicationMapper;
    private final AdoptionRecordMapper recordMapper;
    private final AnimalMapper animalMapper;
    private final OperationLogService logs;
    private final AdoptionResponseAssembler assembler;

    public AdoptionTransactionalService(AdoptionApplicationMapper applicationMapper,AdoptionRecordMapper recordMapper,AnimalMapper animalMapper,
                                        OperationLogService logs,AdoptionResponseAssembler assembler){
        this.applicationMapper=applicationMapper;this.recordMapper=recordMapper;this.animalMapper=animalMapper;this.logs=logs;this.assembler=assembler;
    }

    @Transactional
    public AdoptionApplicationDetailResponse submit(UserPrincipal principal,Long animalId,SubmitAdoptionApplicationRequest request){
        requireApplicantRole(principal); requireId(animalId); NormalizedApplication n=normalize(request);
        Animal animal=animalMapper.selectForUpdate(animalId);
        if(animal==null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if(!"AVAILABLE".equals(animal.getStatus())) throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        AdoptionApplication app=new AdoptionApplication();
        app.setUserId(principal.getUserId()); app.setAnimalId(animalId); app.setAdoptionReason(n.adoptionReason); app.setHousingCondition(n.housingCondition);
        app.setFamilyMembers(n.familyMembers); app.setPetExperience(n.petExperience); app.setContact(n.contact); app.setStatus("PENDING");
        try {
            if(applicationMapper.insert(app)!=1) throw new IllegalStateException("adoption_application insert affected rows != 1");
        } catch(DataIntegrityViolationException ex){
            if(hasConstraint(ex,"uk_adoption_application_user_animal")) throw new BusinessException(ErrorCode.ADOPTION_APPLICATION_ALREADY_EXISTS);
            throw ex;
        }
        logs.append("ADOPTION_APPLICATION",app.getId(),"CREATE",null,"PENDING",principal.getUserId(),null);
        AdoptionApplication saved=applicationMapper.selectById(app.getId());
        if(saved==null) throw new IllegalStateException("new adoption_application cannot be reloaded");
        return assembler.detail(saved,principal);
    }

    @Transactional
    public AdoptionStateActionResponse withdraw(UserPrincipal principal,Long applicationId){
        requireApplicantRole(principal); requireId(applicationId); LocalDateTime now=LocalDateTime.now(TimeUtils.ZONE);
        int rows=applicationMapper.withdraw(applicationId,principal.getUserId(),now);
        if(rows!=1){
            AdoptionApplication existing=applicationMapper.selectById(applicationId);
            if(existing==null || !principal.getUserId().equals(existing.getUserId())) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        }
        logs.append("ADOPTION_APPLICATION",applicationId,"WITHDRAW","PENDING","WITHDRAWN",principal.getUserId(),null);
        AdoptionApplication saved=requiredApplication(applicationId);
        return new AdoptionStateActionResponse(String.valueOf(saved.getId()),saved.getStatus(),TimeUtils.toOffset(saved.getUpdatedAt()));
    }

    @Transactional
    public AdoptionAuditResponse audit(UserPrincipal principal,Long applicationId,AdoptionAuditRequest request){
        requireAdmin(principal); requireId(applicationId);
        AuditPlan plan=validateAudit(request);
        return "REJECT".equals(plan.decision) ? reject(principal,applicationId,plan.rejectReason) : approve(principal,applicationId);
    }

    private AdoptionAuditResponse reject(UserPrincipal admin,Long applicationId,String reason){
        AdoptionApplication target=applicationMapper.selectForUpdate(applicationId);
        if(target==null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if(!"PENDING".equals(target.getStatus())) throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        forbidSelfReview(admin,target);
        LocalDateTime now=LocalDateTime.now(TimeUtils.ZONE);
        if(applicationMapper.rejectPending(applicationId,admin.getUserId(),reason,now)!=1) throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        logs.append("ADOPTION_APPLICATION",applicationId,"AUDIT_REJECT","PENDING","REJECTED",admin.getUserId(),reason);
        AdoptionApplication saved=requiredApplication(applicationId);
        return new AdoptionAuditResponse(assembler.detail(saved,admin),null);
    }

    private AdoptionAuditResponse approve(UserPrincipal admin,Long applicationId){
        Long locatedAnimalId=applicationMapper.selectAnimalIdById(applicationId);
        if(locatedAnimalId==null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        Animal animal=animalMapper.selectForUpdate(locatedAnimalId);
        if(animal==null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        AdoptionApplication target=applicationMapper.selectForUpdate(applicationId);
        if(target==null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if(!Objects.equals(target.getAnimalId(),animal.getId()) || !"PENDING".equals(target.getStatus()) || !"AVAILABLE".equals(animal.getStatus()))
            throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        forbidSelfReview(admin,target);
        List<AdoptionApplication> others=applicationMapper.selectOtherPendingForUpdate(animal.getId(),target.getId());
        LocalDateTime now=LocalDateTime.now(TimeUtils.ZONE);
        if(applicationMapper.approvePending(target.getId(),admin.getUserId(),now)!=1) throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);

        AdoptionRecord record=new AdoptionRecord(); record.setApplicationId(target.getId()); record.setAnimalId(target.getAnimalId()); record.setUserId(target.getUserId()); record.setAdoptedAt(now);
        if(recordMapper.insert(record)!=1) throw new IllegalStateException("adoption_record insert affected rows != 1");
        if(animalMapper.adoptAvailable(animal.getId(),now)!=1) throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        for(AdoptionApplication other:others){
            if(applicationMapper.invalidatePending(other.getId(),now)!=1) throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        }

        logs.append("ADOPTION_APPLICATION",target.getId(),"AUDIT_APPROVE","PENDING","APPROVED",admin.getUserId(),null);
        logs.append("ANIMAL",animal.getId(),"ADOPT","AVAILABLE","ADOPTED",admin.getUserId(),null);
        for(AdoptionApplication other:others){
            logs.append("ADOPTION_APPLICATION",other.getId(),"AUTO_INVALIDATE","PENDING","INVALIDATED",admin.getUserId(),null);
        }
        AdoptionApplication saved=requiredApplication(target.getId());
        AdoptionRecord savedRecord=recordMapper.selectById(record.getId());
        if(savedRecord==null) throw new IllegalStateException("new adoption_record cannot be reloaded");
        return new AdoptionAuditResponse(assembler.detail(saved,admin),assembler.record(savedRecord));
    }

    private void forbidSelfReview(UserPrincipal admin,AdoptionApplication target){
        if(Objects.equals(admin.getUserId(),target.getUserId())) throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    private AuditPlan validateAudit(AdoptionAuditRequest request){
        if(request==null||request.getDecision()==null) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        String decision=request.getDecision().trim();
        if("APPROVE".equals(decision)){
            if(request.isRejectReasonPresent()) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
            return new AuditPlan(decision,null);
        }
        if("REJECT".equals(decision)){
            if(!request.isRejectReasonPresent()) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
            return new AuditPlan(decision,requiredText(request.getRejectReason(),500));
        }
        throw new BusinessException(ErrorCode.INVALID_PARAMETER);
    }
    private NormalizedApplication normalize(SubmitAdoptionApplicationRequest r){
        if(r==null) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        return new NormalizedApplication(requiredText(r.getAdoptionReason(),1000),requiredText(r.getHousingCondition(),1000),requiredText(r.getFamilyMembers(),1000),requiredText(r.getPetExperience(),1000),requiredText(r.getContact(),100));
    }
    private String requiredText(String value,int max){if(value==null) throw new BusinessException(ErrorCode.INVALID_PARAMETER); String v=value.trim(); if(v.isEmpty()||v.length()>max) throw new BusinessException(ErrorCode.INVALID_PARAMETER); return v;}
    private AdoptionApplication requiredApplication(Long id){AdoptionApplication app=applicationMapper.selectById(id); if(app==null) throw new IllegalStateException("adoption_application disappeared"); return app;}
    private void requireApplicantRole(UserPrincipal p){if(p==null || !("USER".equals(p.getRoleCode())||"RESCUER".equals(p.getRoleCode()))) throw new BusinessException(ErrorCode.FORBIDDEN);}
    private void requireAdmin(UserPrincipal p){if(p==null||!"ADMIN".equals(p.getRoleCode())) throw new BusinessException(ErrorCode.FORBIDDEN);}
    private void requireId(Long id){if(id==null||id<=0) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);}
    private boolean hasConstraint(Throwable ex,String name){String needle=name.toLowerCase(Locale.ROOT); for(Throwable t=ex;t!=null;t=t.getCause()){String m=t.getMessage(); if(m!=null&&m.toLowerCase(Locale.ROOT).contains(needle)) return true;} return false;}
    private static class AuditPlan{final String decision,rejectReason; AuditPlan(String d,String r){decision=d;rejectReason=r;}}
    private static class NormalizedApplication{final String adoptionReason,housingCondition,familyMembers,petExperience,contact; NormalizedApplication(String a,String h,String f,String p,String c){adoptionReason=a;housingCondition=h;familyMembers=f;petExperience=p;contact=c;}}
}


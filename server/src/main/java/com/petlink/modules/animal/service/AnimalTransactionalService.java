package com.petlink.modules.animal.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.common.TimeUtils;
import com.petlink.infrastructure.audit.service.OperationLogService;
import com.petlink.infrastructure.file.service.FileStorageService;
import com.petlink.modules.animal.dto.AddHealthRecordRequest;
import com.petlink.modules.animal.dto.AnimalStatusActionRequest;
import com.petlink.modules.animal.dto.AppendAnimalImagesRequest;
import com.petlink.modules.animal.dto.UpdateAnimalRequest;
import com.petlink.modules.animal.entity.Animal;
import com.petlink.modules.animal.entity.AnimalImage;
import com.petlink.modules.animal.entity.HealthRecord;
import com.petlink.modules.animal.mapper.AnimalImageMapper;
import com.petlink.modules.animal.mapper.AnimalMapper;
import com.petlink.modules.animal.mapper.HealthRecordMapper;
import com.petlink.modules.animal.vo.AnimalDetailResponse;
import com.petlink.modules.animal.vo.HealthRecordResponse;
import com.petlink.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class AnimalTransactionalService {
    private static final Logger log=LoggerFactory.getLogger(AnimalTransactionalService.class);
    private static final Set<String> SEXES=Set.of("MALE","FEMALE","UNKNOWN");
    private final AnimalMapper animalMapper;
    private final AnimalImageMapper imageMapper;
    private final HealthRecordMapper healthRecordMapper;
    private final AnimalAccessService access;
    private final AnimalResponseAssembler assembler;
    private final AnimalFileBindingService bindingService;
    private final FileStorageService storage;
    private final OperationLogService operationLogService;

    public AnimalTransactionalService(AnimalMapper animalMapper, AnimalImageMapper imageMapper, HealthRecordMapper healthRecordMapper,
                                      AnimalAccessService access, AnimalResponseAssembler assembler,
                                      AnimalFileBindingService bindingService, FileStorageService storage,
                                      OperationLogService operationLogService) {
        this.animalMapper=animalMapper; this.imageMapper=imageMapper; this.healthRecordMapper=healthRecordMapper;
        this.access=access; this.assembler=assembler; this.bindingService=bindingService; this.storage=storage;
        this.operationLogService=operationLogService;
    }

    @Transactional
    public AnimalDetailResponse update(UserPrincipal principal,Long animalId,UpdateAnimalRequest request) {
        if (request == null || request.getVersion() == null || request.getVersion()<0 || !request.hasAnyBusinessField())
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        Animal current=animalMapper.selectById(animalId);
        if (current == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        access.requireManageAccess(principal,current); // plain-read Task; no Animal lock required for optimistic PATCH
        if (!Objects.equals(current.getVersion(),request.getVersion())) throw new BusinessException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT);

        UpdateWrapper<Animal> update=new UpdateWrapper<>();
        update.eq("id",animalId).eq("version",request.getVersion());
        boolean changed=false;
        if (request.isNamePresent()) { String v=requiredText(request.getName(),100); if (!Objects.equals(v,current.getName())) {update.set("name",v);changed=true;} }
        if (request.isSpeciesPresent()) { String v=requiredText(request.getSpecies(),50); if (!Objects.equals(v,current.getSpecies())) {update.set("species",v);changed=true;} }
        if (request.isSexPresent()) { String v=requiredText(request.getSex(),20); if (!SEXES.contains(v)) throw new BusinessException(ErrorCode.INVALID_PARAMETER); if (!Objects.equals(v,current.getSex())) {update.set("sex",v);changed=true;} }
        if (request.isEstimatedAgeMonthsPresent()) { Integer v=request.getEstimatedAgeMonths(); if (v!=null && (v<0||v>65535)) throw new BusinessException(ErrorCode.INVALID_PARAMETER); if (!Objects.equals(v,current.getEstimatedAgeMonths())) {update.set("estimated_age_months",v);changed=true;} }
        if (request.isColorPresent()) { String v=optionalText(request.getColor(),100); if (!Objects.equals(v,current.getColor())) {update.set("color",v);changed=true;} }
        if (request.isHealthConditionPresent()) { String v=requiredText(request.getHealthCondition(),1000); if (!Objects.equals(v,current.getHealthCondition())) {update.set("health_condition",v);changed=true;} }
        if (request.isPersonalityPresent()) { String v=optionalText(request.getPersonality(),1000); if (!Objects.equals(v,current.getPersonality())) {update.set("personality",v);changed=true;} }
        if (request.isAdoptionRequirementsPresent()) { String v=optionalText(request.getAdoptionRequirements(),1000); if (!Objects.equals(v,current.getAdoptionRequirements())) {update.set("adoption_requirements",v);changed=true;} }
        if (!changed) return assembler.detail(current,true);
        update.setSql("version = version + 1").set("updated_at",LocalDateTime.now(TimeUtils.ZONE));
        if (animalMapper.update(null,update) != 1) distinguishOptimistic(principal,animalId,request.getVersion(),null);
        Animal saved=animalMapper.selectById(animalId);
        return assembler.detail(saved,true);
    }

    @Transactional
    public AnimalDetailResponse addImages(UserPrincipal principal,Long animalId,AppendAnimalImagesRequest request) {
        List<String> tokens=validateTokens(request == null ? null : request.getImageTokens());
        Animal animal=animalMapper.selectForUpdate(animalId);
        if (animal == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        access.requireManageAccess(principal,animal);
        int start=imageMapper.maxSortOrder(animalId)+1;
        AnimalFileBindingService.PreparedBindings prepared=bindingService.lockAndValidate(principal.getUserId(),tokens);
        bindingService.bindPreparedStartingAt(principal.getUserId(),prepared,animalId,tokens,start);
        bindingService.copyPrepared(prepared);
        return assembler.detail(animalMapper.selectById(animalId),true);
    }

    @Transactional
    public AnimalDetailResponse deleteImage(UserPrincipal principal,Long animalId,Long imageId) {
        Animal animal=animalMapper.selectForUpdate(animalId);
        if (animal == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        access.requireManageAccess(principal,animal);
        AnimalImage target=imageMapper.selectById(imageId);
        if (target == null || !animalId.equals(target.getAnimalId())) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        List<AnimalImage> after=imageMapper.selectAfter(animalId,target.getSortOrder());
        if (imageMapper.deleteById(imageId) != 1) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        for (AnimalImage image : after) {
            if (imageMapper.moveSortOrder(image.getId(),image.getSortOrder(),image.getSortOrder()-1) != 1)
                throw new IllegalStateException("animal image sort reorder affected rows != 1");
        }
        registerFormalDeleteAfterCommit(target.getImagePath());
        return assembler.detail(animalMapper.selectById(animalId),true);
    }

    @Transactional
    public HealthRecordResponse addHealthRecord(UserPrincipal principal,Long animalId,AddHealthRecordRequest request) {
        String content=requiredText(request == null ? null : request.getContent(),2000);
        Animal animal=animalMapper.selectForUpdate(animalId);
        if (animal == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        access.requireManageAccess(principal,animal);
        HealthRecord record=new HealthRecord(); record.setAnimalId(animalId); record.setRecorderId(principal.getUserId()); record.setContent(content);
        if (healthRecordMapper.insert(record) != 1) throw new IllegalStateException("health_record insert affected rows != 1");
        HealthRecord saved=healthRecordMapper.selectById(record.getId());
        return assembler.fullHealth(saved == null ? record : saved);
    }

    @Transactional
    public AnimalDetailResponse statusAction(UserPrincipal principal,Long animalId,AnimalStatusActionRequest request) {
        StatusPlan plan=validateStatusAction(request);
        Animal current=animalMapper.selectById(animalId);
        if (current == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        access.requireManageAccess(principal,current);
        if (!plan.before.equals(current.getStatus())) throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        if (!Objects.equals(current.getVersion(),request.getVersion())) throw new BusinessException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT);

        UpdateWrapper<Animal> update=new UpdateWrapper<>();
        update.eq("id",animalId).eq("status",plan.before).eq("version",request.getVersion())
                .set("status",plan.after).set("suspend_reason",plan.suspendReason)
                .setSql("version = version + 1").set("updated_at",LocalDateTime.now(TimeUtils.ZONE));
        if (animalMapper.update(null,update) != 1) distinguishOptimistic(principal,animalId,request.getVersion(),plan.before);
        operationLogService.append("ANIMAL",animalId,plan.operation,plan.before,plan.after,principal.getUserId(),plan.logReason);
        return assembler.detail(animalMapper.selectById(animalId),true);
    }

    private void distinguishOptimistic(UserPrincipal principal,Long animalId,Integer expectedVersion,String expectedStatus) {
        Animal latest=animalMapper.selectById(animalId);
        if (latest == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        access.requireManageAccess(principal,latest);
        if (expectedStatus != null && !expectedStatus.equals(latest.getStatus())) throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        if (!Objects.equals(expectedVersion,latest.getVersion())) throw new BusinessException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
    }

    private StatusPlan validateStatusAction(AnimalStatusActionRequest request) {
        if (request == null || request.getAction()==null || request.getVersion()==null || request.getVersion()<0)
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        String action=request.getAction().trim();
        switch (action) {
            case "TO_OBSERVING":
                forbidReason(request); return new StatusPlan("TREATING","OBSERVING","TO_OBSERVING",null,null);
            case "OPEN_ADOPTION":
                forbidReason(request); return new StatusPlan("OBSERVING","AVAILABLE","OPEN_ADOPTION",null,null);
            case "SUSPEND_ADOPTION":
                if (!request.isSuspendReasonPresent()) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
                String reason=requiredText(request.getSuspendReason(),500);
                return new StatusPlan("AVAILABLE","SUSPENDED","SUSPEND_ADOPTION",reason,reason);
            case "RESUME_ADOPTION":
                forbidReason(request); return new StatusPlan("SUSPENDED","AVAILABLE","RESUME_ADOPTION",null,null);
            default: throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
    }
    private void forbidReason(AnimalStatusActionRequest request) {
        if (request.isSuspendReasonPresent()) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
    }
    private List<String> validateTokens(List<String> tokens) {
        if (tokens == null || tokens.isEmpty() || tokens.size()>9) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        Set<String> seen=new HashSet<>();
        for (String token : tokens) {
            if (token == null || token.isBlank() || !seen.add(token)) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        return List.copyOf(tokens);
    }
    private String requiredText(String value,int max) {
        if (value == null) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        String v=value.trim(); if (v.isEmpty() || v.length()>max) throw new BusinessException(ErrorCode.INVALID_PARAMETER); return v;
    }
    private String optionalText(String value,int max) {
        if (value == null) return null;
        String v=value.trim(); if (v.isEmpty() || v.length()>max) throw new BusinessException(ErrorCode.INVALID_PARAMETER); return v;
    }
    private void registerFormalDeleteAfterCommit(String path) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) throw new IllegalStateException("transaction synchronization required");
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                if (!storage.deleteIfExists(path,"animal image delete after commit"))
                    log.error("Formal animal image deletion failed after DB commit; orphan scanner must retry: {}",path);
            }
        });
    }
    private static class StatusPlan {
        private final String before,after,operation,suspendReason,logReason;
        private StatusPlan(String before,String after,String operation,String suspendReason,String logReason) {
            this.before=before; this.after=after; this.operation=operation; this.suspendReason=suspendReason; this.logReason=logReason;
        }
    }
}

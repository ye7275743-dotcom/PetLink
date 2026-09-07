package com.petlink.modules.rescue.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.common.TimeUtils;
import com.petlink.infrastructure.audit.service.OperationLogService;
import com.petlink.modules.animal.entity.Animal;
import com.petlink.modules.animal.entity.HealthRecord;
import com.petlink.modules.animal.mapper.AnimalMapper;
import com.petlink.modules.animal.mapper.HealthRecordMapper;
import com.petlink.modules.animal.service.AnimalFileBindingService;
import com.petlink.modules.clue.entity.RescueClue;
import com.petlink.modules.clue.mapper.RescueClueMapper;
import com.petlink.modules.clue.vo.StateActionResponse;
import com.petlink.modules.rescue.dto.CancelRescueTaskRequest;
import com.petlink.modules.rescue.dto.FailureResolutionRequest;
import com.petlink.modules.rescue.entity.RescueRecord;
import com.petlink.modules.rescue.entity.RescueTask;
import com.petlink.modules.rescue.mapper.RescueRecordMapper;
import com.petlink.modules.rescue.mapper.RescueTaskMapper;
import com.petlink.modules.rescue.service.RescueRequestNormalizer.NormalizedAnimal;
import com.petlink.modules.rescue.service.RescueRequestNormalizer.NormalizedResult;
import com.petlink.modules.rescue.vo.AcceptTaskResponse;
import com.petlink.modules.rescue.vo.RescueRecordResponse;
import com.petlink.modules.rescue.vo.RescueSuccessResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RescueTaskTransactionalService {
    private final RescueTaskMapper taskMapper;
    private final RescueRecordMapper recordMapper;
    private final RescueClueMapper clueMapper;
    private final AnimalMapper animalMapper;
    private final HealthRecordMapper healthRecordMapper;
    private final AnimalFileBindingService animalFileBindingService;
    private final OperationLogService operationLogService;
    private final RescueTaskResponseAssembler assembler;
    private final RescueRequestNormalizer normalizer;

    public RescueTaskTransactionalService(RescueTaskMapper taskMapper, RescueRecordMapper recordMapper,
                                          RescueClueMapper clueMapper, AnimalMapper animalMapper,
                                          HealthRecordMapper healthRecordMapper, AnimalFileBindingService animalFileBindingService,
                                          OperationLogService operationLogService, RescueTaskResponseAssembler assembler,
                                          RescueRequestNormalizer normalizer) {
        this.taskMapper=taskMapper; this.recordMapper=recordMapper; this.clueMapper=clueMapper;
        this.animalMapper=animalMapper; this.healthRecordMapper=healthRecordMapper;
        this.animalFileBindingService=animalFileBindingService; this.operationLogService=operationLogService;
        this.assembler=assembler; this.normalizer=normalizer;
    }

    @Transactional
    public AcceptTaskResponse accept(Long rescuerId, Long clueId) {
        if (clueId == null || clueId <= 0) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if (taskMapper.lockEnabledRescuer(rescuerId)==null) throw new BusinessException(ErrorCode.FORBIDDEN,"账号权限已变化，请刷新后重试");
        int rows=clueMapper.acceptForRescue(clueId);
        if (rows != 1) {
            RescueClue clue=clueMapper.selectById(clueId);
            if (clue == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        }
        RescueTask task=new RescueTask();
        task.setClueId(clueId); task.setRescuerId(rescuerId); task.setStatus("WAITING_START");
        try {
            if (taskMapper.insert(task) != 1) throw new IllegalStateException("rescue_task insert affected rows != 1");
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        }
        operationLogService.append("RESCUE_CLUE", clueId, "ACCEPT_RESCUE", "WAITING_ACCEPT", "CONVERTED", rescuerId, null);
        RescueTask saved=taskMapper.selectById(task.getId());
        if (saved == null) throw new IllegalStateException("new rescue_task cannot be reloaded");
        return new AcceptTaskResponse(String.valueOf(saved.getId()), String.valueOf(saved.getClueId()), saved.getStatus(), TimeUtils.toOffset(saved.getCreatedAt()));
    }

    @Transactional
    public StateActionResponse start(Long rescuerId, Long taskId) {
        LocalDateTime now=LocalDateTime.now(TimeUtils.ZONE);
        int rows=taskMapper.start(taskId,rescuerId,now);
        if (rows != 1) distinguishOwnedTaskConflict(rescuerId,taskId);
        operationLogService.append("RESCUE_TASK", taskId, "START_RESCUE", "WAITING_START", "IN_PROGRESS", rescuerId, null);
        return state(taskMapper.selectById(taskId));
    }

    @Transactional
    public RescueRecordResponse addRecord(Long rescuerId, Long taskId, String contentRaw) {
        String content=normalizer.requiredText(contentRaw,2000);
        RescueTask task=taskMapper.selectForUpdate(taskId);
        if (task == null || !rescuerId.equals(task.getRescuerId())) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if (!"IN_PROGRESS".equals(task.getStatus())) throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        RescueRecord record=new RescueRecord();
        record.setTaskId(taskId); record.setRecorderId(rescuerId); record.setContent(content);
        if (recordMapper.insert(record) != 1) throw new IllegalStateException("rescue_record insert affected rows != 1");
        RescueRecord saved=recordMapper.selectById(record.getId());
        if (saved == null) throw new IllegalStateException("new rescue_record cannot be reloaded");
        return assembler.record(saved);
    }

    @Transactional
    public Object submitResult(Long rescuerId, Long taskId, NormalizedResult request) {
        if ("FAILED".equals(request.getResult())) return fail(rescuerId,taskId,request.getFailureReason());
        return succeed(rescuerId,taskId,request.getAnimals());
    }

    private StateActionResponse fail(Long rescuerId, Long taskId, String reason) {
        LocalDateTime now=LocalDateTime.now(TimeUtils.ZONE);
        int rows=taskMapper.fail(taskId,rescuerId,reason,now);
        if (rows != 1) distinguishOwnedTaskConflict(rescuerId,taskId);
        operationLogService.append("RESCUE_TASK", taskId, "RESCUE_FAILED", "IN_PROGRESS", "FAILED", rescuerId, reason);
        return state(taskMapper.selectById(taskId));
    }

    private RescueSuccessResponse succeed(Long rescuerId, Long taskId, List<NormalizedAnimal> requests) {
        // Frozen lock order: Task -> Clue -> TemporaryFile by id ASC.
        RescueTask task=taskMapper.selectForUpdate(taskId);
        if (task == null || !rescuerId.equals(task.getRescuerId())) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if (!"IN_PROGRESS".equals(task.getStatus())) throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        Long clueId=task.getClueId();
        RescueClue clue=clueMapper.selectForUpdate(clueId);
        if (clue == null) throw new IllegalStateException("rescue_task references missing rescue_clue");
        if (!"CONVERTED".equals(clue.getStatus())) throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);

        List<String> allTokens=requests.stream().flatMap(a -> a.getImageTokens().stream()).collect(Collectors.toList());
        AnimalFileBindingService.PreparedBindings prepared=animalFileBindingService.lockAndValidate(rescuerId,allTokens);

        List<Animal> animals=new ArrayList<>();
        List<AnimalFileBindingService.AnimalTokens> tokenGroups=new ArrayList<>();
        for (NormalizedAnimal request : requests) {
            Animal animal=new Animal();
            animal.setRescueTaskId(taskId); animal.setName(request.getName()); animal.setSpecies(request.getSpecies());
            animal.setSex(request.getSex()); animal.setEstimatedAgeMonths(request.getEstimatedAgeMonths()); animal.setColor(request.getColor());
            animal.setHealthCondition(request.getHealthCondition()); animal.setStatus("TREATING"); animal.setSuspendReason(null); animal.setVersion(0);
            if (animalMapper.insert(animal) != 1) throw new IllegalStateException("animal insert affected rows != 1");
            animals.add(animal);
            if (request.getInitialHealthRecord() != null) {
                HealthRecord record=new HealthRecord();
                record.setAnimalId(animal.getId()); record.setRecorderId(rescuerId); record.setContent(request.getInitialHealthRecord());
                if (healthRecordMapper.insert(record) != 1) throw new IllegalStateException("health_record insert affected rows != 1");
            }
            tokenGroups.add(new AnimalFileBindingService.AnimalTokens(animal.getId(), request.getImageTokens()));
        }

        animalFileBindingService.bindPrepared(rescuerId,prepared,tokenGroups);
        LocalDateTime finishedAt=LocalDateTime.now(TimeUtils.ZONE);
        if (taskMapper.succeed(taskId,rescuerId,finishedAt) != 1) throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        if (clueMapper.closeConverted(clueId) != 1) throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        operationLogService.append("RESCUE_TASK", taskId, "COMPLETE_RESCUE", "IN_PROGRESS", "SUCCESS", rescuerId, null);
        operationLogService.append("RESCUE_CLUE", clueId, "CLOSE_AFTER_RESCUE", "CONVERTED", "CLOSED", rescuerId, null);
        animalFileBindingService.copyPrepared(prepared);

        return new RescueSuccessResponse(String.valueOf(taskId), "SUCCESS",
                animals.stream().map(a -> String.valueOf(a.getId())).collect(Collectors.toList()), TimeUtils.toOffset(finishedAt));
    }

    @Transactional
    public StateActionResponse cancel(Long adminId, Long taskId, CancelRescueTaskRequest request) {
        String reason=normalizer.requiredText(request == null ? null : request.getCancelReason(),500);
        RescueTask task=taskMapper.selectForUpdate(taskId);
        if (task == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        String before=task.getStatus();
        if (!("WAITING_START".equals(before)||"IN_PROGRESS".equals(before))) throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        RescueClue clue=clueMapper.selectForUpdate(task.getClueId());
        if (clue == null) throw new IllegalStateException("rescue_task references missing rescue_clue");
        if (!"CONVERTED".equals(clue.getStatus())) throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        LocalDateTime now=LocalDateTime.now(TimeUtils.ZONE);
        if (taskMapper.cancel(taskId,reason,now) != 1) throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        if (clueMapper.reopenConverted(clue.getId()) != 1) throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        operationLogService.append("RESCUE_TASK",taskId,"CANCEL_RESCUE",before,"CANCELED",adminId,reason);
        operationLogService.append("RESCUE_CLUE",clue.getId(),"REOPEN","CONVERTED","WAITING_ACCEPT",adminId,reason);
        return state(taskMapper.selectById(taskId));
    }

    @Transactional
    public StateActionResponse resolveFailure(Long adminId, Long taskId, FailureResolutionRequest request) {
        if (request == null || request.getAction() == null) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        String action=request.getAction().trim();
        if (!("REOPEN".equals(action)||"CLOSE".equals(action))) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        String reason=normalizer.requiredText(request.getResolutionReason(),500);

        RescueTask task=taskMapper.selectForUpdate(taskId);
        if (task == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if (!"FAILED".equals(task.getStatus())) throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        RescueClue clue=clueMapper.selectForUpdate(task.getClueId());
        if (clue == null) throw new IllegalStateException("rescue_task references missing rescue_clue");
        if (!"CONVERTED".equals(clue.getStatus())) throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);

        RescueTask latest=taskMapper.selectLatestForUpdateByClue(clue.getId());
        if (latest == null || !taskId.equals(latest.getId())) throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        RescueTask active=taskMapper.selectActiveForUpdateByClue(clue.getId());
        if (active != null) throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);

        String after;
        String operation;
        if ("REOPEN".equals(action)) {
            if (clueMapper.reopenConverted(clue.getId()) != 1) throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
            after="WAITING_ACCEPT"; operation="REOPEN";
        } else {
            if (clueMapper.closeConverted(clue.getId()) != 1) throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
            after="CLOSED"; operation="CLOSE_AFTER_FAILURE";
        }
        operationLogService.append("RESCUE_CLUE",clue.getId(),operation,"CONVERTED",after,adminId,reason);
        RescueClue saved=clueMapper.selectById(clue.getId());
        return new StateActionResponse(String.valueOf(saved.getId()),saved.getStatus(),TimeUtils.toOffset(saved.getUpdatedAt()));
    }

    private void distinguishOwnedTaskConflict(Long rescuerId, Long taskId) {
        RescueTask task=taskMapper.selectById(taskId);
        if (task == null || !rescuerId.equals(task.getRescuerId())) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
    }
    private StateActionResponse state(RescueTask task) {
        if (task == null) throw new IllegalStateException("state action resource cannot be reloaded");
        return new StateActionResponse(String.valueOf(task.getId()),task.getStatus(),TimeUtils.toOffset(task.getUpdatedAt()));
    }
}

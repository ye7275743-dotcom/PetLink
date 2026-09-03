package com.petlink.modules.clue.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.common.TimeUtils;
import com.petlink.infrastructure.audit.service.OperationLogService;
import com.petlink.infrastructure.file.service.FileStorageService;
import com.petlink.modules.clue.dto.AuditClueRequest;
import com.petlink.modules.clue.entity.RescueClue;
import com.petlink.modules.clue.entity.RescueClueImage;
import com.petlink.modules.clue.mapper.RescueClueImageMapper;
import com.petlink.modules.clue.mapper.RescueClueMapper;
import com.petlink.modules.clue.service.ClueRequestNormalizer.NormalizedCreate;
import com.petlink.modules.clue.service.ClueRequestNormalizer.NormalizedUpdate;
import com.petlink.modules.clue.vo.ClueDetailResponse;
import com.petlink.modules.clue.vo.CreateClueResponse;
import com.petlink.modules.clue.vo.StateActionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class RescueClueTransactionalService {
    private static final Logger log = LoggerFactory.getLogger(RescueClueTransactionalService.class);
    private final RescueClueMapper clueMapper;
    private final RescueClueImageMapper imageMapper;
    private final ClueFileBindingService fileBindingService;
    private final FileStorageService fileStorageService;
    private final OperationLogService operationLogService;
    private final ClueResponseAssembler assembler;
    private final ClueIdempotencyStore idempotencyStore;

    public RescueClueTransactionalService(RescueClueMapper clueMapper,
                                          RescueClueImageMapper imageMapper,
                                          ClueFileBindingService fileBindingService,
                                          FileStorageService fileStorageService,
                                          OperationLogService operationLogService,
                                          ClueResponseAssembler assembler,
                                          ClueIdempotencyStore idempotencyStore) {
        this.clueMapper = clueMapper;
        this.imageMapper = imageMapper;
        this.fileBindingService = fileBindingService;
        this.fileStorageService = fileStorageService;
        this.operationLogService = operationLogService;
        this.assembler = assembler;
        this.idempotencyStore = idempotencyStore;
    }

    @Transactional
    public CreateClueResponse create(Long userId, String idempotencyKey, NormalizedCreate request) {
        AtomicReference<CreateClueResponse> committedResponse = new AtomicReference<>();
        registerIdempotencyAfterCommit(userId, idempotencyKey, request.getFingerprint(), committedResponse);

        RescueClue clue = new RescueClue();
        clue.setPublisherId(userId);
        clue.setLocation(request.getLocation());
        clue.setFoundTime(request.getFoundTime());
        clue.setAnimalDescription(request.getAnimalDescription());
        clue.setSceneDescription(request.getSceneDescription());
        clue.setContact(request.getContact());
        clue.setStatus("PENDING_REVIEW");
        if (clueMapper.insert(clue) != 1) {
            throw new IllegalStateException("rescue_clue insert affected rows != 1");
        }

        ClueFileBindingService.PreparedBindings prepared = fileBindingService.prepareBindings(
                userId, clue.getId(), request.getImageTokens(), 1);
        operationLogService.append("RESCUE_CLUE", clue.getId(), "CREATE",
                null, "PENDING_REVIEW", userId, null);
        // Frozen create order: DB image/binding writes -> CREATE log -> formal copies -> COMMIT.
        fileBindingService.copyPrepared(prepared);

        RescueClue saved = clueMapper.selectById(clue.getId());
        if (saved == null) throw new IllegalStateException("new rescue_clue cannot be reloaded");
        CreateClueResponse response = new CreateClueResponse(
                String.valueOf(saved.getId()), saved.getStatus(), TimeUtils.toOffset(saved.getCreatedAt()));
        committedResponse.set(response);
        return response;
    }

    @Transactional
    public ClueDetailResponse update(Long userId, Long clueId, NormalizedUpdate request) {
        RescueClue clue = lockOwnedPending(userId, clueId);

        UpdateWrapper<RescueClue> update = new UpdateWrapper<RescueClue>().eq("id", clueId);
        boolean changed = false;

        if (request.isLocationPresent() && !Objects.equals(clue.getLocation(), request.getLocation())) {
            update.set("location", request.getLocation());
            changed = true;
        }
        if (request.isFoundTimePresent() && !Objects.equals(clue.getFoundTime(), request.getFoundTime())) {
            update.set("found_time", request.getFoundTime());
            changed = true;
        }
        if (request.isAnimalDescriptionPresent() && !Objects.equals(clue.getAnimalDescription(), request.getAnimalDescription())) {
            update.set("animal_description", request.getAnimalDescription());
            changed = true;
        }
        if (request.isSceneDescriptionPresent() && !Objects.equals(clue.getSceneDescription(), request.getSceneDescription())) {
            // UpdateWrapper.set persists SQL NULL explicitly, matching Frozen PATCH null semantics.
            update.set("scene_description", request.getSceneDescription());
            changed = true;
        }
        if (request.isContactPresent() && !Objects.equals(clue.getContact(), request.getContact())) {
            update.set("contact", request.getContact());
            changed = true;
        }

        // Frozen rule: a same-value PATCH is a valid no-op. Do not execute UPDATE at all.
        if (changed) {
            int rows = clueMapper.update(null, update);
            if (rows != 1) {
                throw new IllegalStateException("rescue_clue PATCH affected rows != 1 after row lock");
            }
        }

        RescueClue saved = changed ? clueMapper.selectById(clueId) : clue;
        return assembler.detail(saved, false);
    }

    @Transactional
    public ClueDetailResponse appendImages(Long userId, Long clueId, List<String> tokens) {
        lockOwnedPending(userId, clueId);
        int existing = imageMapper.countByClueId(clueId);
        if (existing < 1) {
            throw new IllegalStateException("Frozen invariant violated: rescue clue has no image");
        }
        if (existing + tokens.size() > 9) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        int maxSortOrder = imageMapper.selectMaxSortOrder(clueId);
        if (maxSortOrder < 1) {
            throw new IllegalStateException("Frozen invariant violated: rescue clue image sort_order is invalid");
        }
        fileBindingService.bindImages(userId, clueId, tokens, maxSortOrder + 1);
        return assembler.detail(clueMapper.selectById(clueId), false);
    }

    @Transactional
    public ClueDetailResponse deleteImage(Long userId, Long clueId, Long imageId) {
        lockOwnedPending(userId, clueId);
        List<RescueClueImage> images = imageMapper.selectByClueId(clueId);
        if (images.size() <= 1) {
            throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT, "救助线索必须至少保留一张图片");
        }
        RescueClueImage target = images.stream()
                .filter(image -> imageId != null && imageId.equals(image.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (imageMapper.deleteById(target.getId()) != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        }
        List<RescueClueImage> later = images.stream()
                .filter(image -> image.getSortOrder() > target.getSortOrder())
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .toList();
        for (RescueClueImage image : later) {
            if (imageMapper.shiftOneForward(image.getId()) != 1) {
                throw new IllegalStateException("rescue_clue_image reorder affected rows != 1");
            }
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                fileStorageService.deleteQuietly(target.getImagePath(), "deleted rescue clue image");
            }
        });

        return assembler.detail(clueMapper.selectById(clueId), false);
    }

    @Transactional
    public StateActionResponse withdraw(Long userId, Long clueId) {
        int rows = clueMapper.withdrawPending(clueId, userId);
        if (rows != 1) {
            RescueClue existing = clueMapper.selectById(clueId);
            if (existing == null || !userId.equals(existing.getPublisherId())) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            }
            throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        }
        operationLogService.append("RESCUE_CLUE", clueId, "WITHDRAW",
                "PENDING_REVIEW", "WITHDRAWN", userId, null);
        RescueClue saved = clueMapper.selectById(clueId);
        return state(saved);
    }

    @Transactional
    public StateActionResponse audit(Long adminId, Long clueId, AuditClueRequest request) {
        if (request == null || request.getDecision() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        String decision = request.getDecision().trim();
        LocalDateTime reviewedAt = LocalDateTime.now(TimeUtils.ZONE);
        int rows;
        String afterStatus;
        String operationType;
        String reason = null;

        if ("APPROVE".equals(decision)) {
            if (request.getRejectReason() != null) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER);
            }
            rows = clueMapper.auditApprove(clueId, adminId, reviewedAt);
            afterStatus = "WAITING_ACCEPT";
            operationType = "AUDIT_APPROVE";
        } else if ("REJECT".equals(decision)) {
            reason = normalizeRejectReason(request.getRejectReason());
            rows = clueMapper.auditReject(clueId, adminId, reviewedAt, reason);
            afterStatus = "REJECTED";
            operationType = "AUDIT_REJECT";
        } else {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }

        if (rows != 1) {
            RescueClue existing = clueMapper.selectById(clueId);
            if (existing == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        }

        operationLogService.append("RESCUE_CLUE", clueId, operationType,
                "PENDING_REVIEW", afterStatus, adminId, reason);
        return state(clueMapper.selectById(clueId));
    }

    private RescueClue lockOwnedPending(Long userId, Long clueId) {
        if (clueId == null || clueId <= 0) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        RescueClue clue = clueMapper.selectForUpdate(clueId);
        if (clue == null || !userId.equals(clue.getPublisherId())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (!"PENDING_REVIEW".equals(clue.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        }
        return clue;
    }

    private String normalizeRejectReason(String value) {
        if (value == null) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        String reason = value.trim();
        if (reason.isEmpty() || reason.length() > 500) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        return reason;
    }

    private StateActionResponse state(RescueClue clue) {
        if (clue == null) throw new IllegalStateException("rescue_clue cannot be reloaded");
        return new StateActionResponse(String.valueOf(clue.getId()), clue.getStatus(), TimeUtils.toOffset(clue.getUpdatedAt()));
    }

    private void registerIdempotencyAfterCommit(Long userId, String key, String fingerprint,
                                                AtomicReference<CreateClueResponse> responseRef) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Transaction synchronization is required for clue creation");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    CreateClueResponse response = responseRef.get();
                    if (response == null) {
                        log.error("Committed clue response missing for idempotency key: userId={}, key={}", userId, key);
                        return;
                    }
                    idempotencyStore.markSucceeded(userId, key, fingerprint, response);
                } catch (RuntimeException ex) {
                    // The business transaction is already committed. Never surface this callback failure as if the create rolled back.
                    log.error("Failed to mark committed clue idempotency key as SUCCEEDED: userId={}, key={}", userId, key, ex);
                }
            }
        });
    }
}

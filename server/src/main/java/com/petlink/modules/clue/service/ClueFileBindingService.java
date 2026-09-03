package com.petlink.modules.clue.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.common.TimeUtils;
import com.petlink.infrastructure.file.entity.TemporaryFile;
import com.petlink.infrastructure.file.mapper.TemporaryFileMapper;
import com.petlink.infrastructure.file.service.BoundTemporaryFileCleanupService;
import com.petlink.infrastructure.file.service.FileStorageService;
import com.petlink.modules.clue.entity.RescueClueImage;
import com.petlink.modules.clue.mapper.RescueClueImageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ClueFileBindingService {
    private static final Logger log = LoggerFactory.getLogger(ClueFileBindingService.class);
    private static final long MAX_SIZE = 5L * 1024L * 1024L;

    private final TemporaryFileMapper temporaryFileMapper;
    private final RescueClueImageMapper imageMapper;
    private final FileStorageService storage;
    private final BoundTemporaryFileCleanupService cleanupService;

    public ClueFileBindingService(TemporaryFileMapper temporaryFileMapper,
                                  RescueClueImageMapper imageMapper,
                                  FileStorageService storage,
                                  BoundTemporaryFileCleanupService cleanupService) {
        this.temporaryFileMapper = temporaryFileMapper;
        this.imageMapper = imageMapper;
        this.storage = storage;
        this.cleanupService = cleanupService;
    }

    /**
     * Convenience path used when no other same-transaction write has to be inserted between
     * database binding and physical copy (for example append-images).
     */
    public void bindImages(Long ownerId, Long clueId, List<String> tokens, int startSortOrder) {
        PreparedBindings prepared = prepareBindings(ownerId, clueId, tokens, startSortOrder);
        copyPrepared(prepared);
    }

    /**
     * Phase 1: lock temporary rows by id ASC; insert image rows and conditionally bind temporary_file rows.
     * Image sort order still follows the original client token order.
     */
    public PreparedBindings prepareBindings(Long ownerId, Long clueId, List<String> tokens, int startSortOrder) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Transaction synchronization is required for file binding");
        }
        if (tokens == null || tokens.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }

        List<TemporaryFile> locked = temporaryFileMapper.selectForUpdateByTokens(tokens);
        if (locked.size() != tokens.size()) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }

        LocalDateTime now = LocalDateTime.now(TimeUtils.ZONE);
        Map<String, TemporaryFile> byToken = new HashMap<>();
        for (TemporaryFile file : locked) {
            validateLockedTemporaryFile(file, ownerId, now);
            byToken.put(file.getToken(), file);
        }
        if (byToken.size() != tokens.size()) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }

        List<BoundFile> boundFiles = new ArrayList<>();
        List<String> createdFormalPaths = new ArrayList<>();
        registerSynchronization(boundFiles, createdFormalPaths);

        int sortOrder = startSortOrder;
        for (String token : tokens) {
            TemporaryFile temp = byToken.get(token);
            if (temp == null) {
                throw new BusinessException(ErrorCode.INVALID_FILE);
            }
            String formalPath = buildFormalPath(clueId, temp.getFileExtension());

            RescueClueImage image = new RescueClueImage();
            image.setClueId(clueId);
            image.setImagePath(formalPath);
            image.setSortOrder(sortOrder++);
            if (imageMapper.insert(image) != 1) {
                throw new IllegalStateException("rescue_clue_image insert affected rows != 1");
            }

            int rows = temporaryFileMapper.bindUploaded(
                    temp.getId(), ownerId, "RESCUE_CLUE", clueId, formalPath, now);
            if (rows != 1) {
                throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT,
                        "临时文件已失效或被其他业务使用");
            }
            boundFiles.add(new BoundFile(temp.getId(), temp.getTempPath(), clueId, formalPath));
        }
        return new PreparedBindings(boundFiles, createdFormalPaths);
    }

    /**
     * Phase 2: copy temp originals to the already-bound formal paths before transaction commit.
     * Rollback/commit-failure cleanup is handled by the synchronization registered in phase 1.
     */
    public void copyPrepared(PreparedBindings prepared) {
        if (prepared == null) throw new IllegalArgumentException("prepared bindings required");
        for (BoundFile bound : prepared.boundFiles) {
            storage.copyTemporaryToFormal(bound.tempPath, bound.formalPath);
            prepared.createdFormalPaths.add(bound.formalPath);
        }
    }

    private void validateLockedTemporaryFile(TemporaryFile file, Long ownerId, LocalDateTime now) {
        if (file == null
                || !ownerId.equals(file.getOwnerId())
                || !"UPLOADED".equals(file.getStatus())
                || file.getExpiresAt() == null
                || !file.getExpiresAt().isAfter(now)
                || file.getFileSizeBytes() == null
                || file.getFileSizeBytes() <= 0
                || file.getFileSizeBytes() > MAX_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }
        String ext = file.getFileExtension();
        String mime = file.getMimeType();
        boolean validType = (("jpg".equals(ext) || "jpeg".equals(ext)) && "image/jpeg".equals(mime))
                || ("png".equals(ext) && "image/png".equals(mime));
        if (!validType) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }
    }

    private String buildFormalPath(Long clueId, String extension) {
        String uuid = UUID.randomUUID().toString().toLowerCase(Locale.ROOT);
        return "rescue-clues/" + clueId + "/" + uuid + "." + extension;
    }

    private void registerSynchronization(List<BoundFile> boundFiles, List<String> createdFormalPaths) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (BoundFile bound : boundFiles) {
                    try {
                        cleanupService.cleanupOne(bound.temporaryFileId, bound.tempPath,
                                "RESCUE_CLUE", bound.clueId, bound.formalPath);
                    } catch (RuntimeException ex) {
                        log.error("Post-commit temporary cleanup callback failed: tempId={}", bound.temporaryFileId, ex);
                    }
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    for (String formalPath : createdFormalPaths) {
                        storage.deleteQuietly(formalPath, "rescue clue transaction rollback");
                    }
                }
            }
        });
    }

    public static class PreparedBindings {
        private final List<BoundFile> boundFiles;
        private final List<String> createdFormalPaths;

        private PreparedBindings(List<BoundFile> boundFiles, List<String> createdFormalPaths) {
            this.boundFiles = boundFiles;
            this.createdFormalPaths = createdFormalPaths;
        }
    }

    private static class BoundFile {
        private final Long temporaryFileId;
        private final String tempPath;
        private final Long clueId;
        private final String formalPath;

        private BoundFile(Long temporaryFileId, String tempPath, Long clueId, String formalPath) {
            this.temporaryFileId = temporaryFileId;
            this.tempPath = tempPath;
            this.clueId = clueId;
            this.formalPath = formalPath;
        }
    }
}

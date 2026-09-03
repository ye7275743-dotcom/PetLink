package com.petlink.modules.animal.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.common.TimeUtils;
import com.petlink.infrastructure.file.entity.TemporaryFile;
import com.petlink.infrastructure.file.mapper.TemporaryFileMapper;
import com.petlink.infrastructure.file.service.BoundTemporaryFileCleanupService;
import com.petlink.infrastructure.file.service.FileStorageService;
import com.petlink.modules.animal.entity.AnimalImage;
import com.petlink.modules.animal.mapper.AnimalImageMapper;
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
public class AnimalFileBindingService {
    private static final Logger log = LoggerFactory.getLogger(AnimalFileBindingService.class);
    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private final TemporaryFileMapper temporaryFileMapper;
    private final AnimalImageMapper imageMapper;
    private final FileStorageService storage;
    private final BoundTemporaryFileCleanupService cleanupService;

    public AnimalFileBindingService(TemporaryFileMapper temporaryFileMapper, AnimalImageMapper imageMapper,
                                    FileStorageService storage, BoundTemporaryFileCleanupService cleanupService) {
        this.temporaryFileMapper=temporaryFileMapper; this.imageMapper=imageMapper;
        this.storage=storage; this.cleanupService=cleanupService;
    }

    /**
     * Frozen SUCCESS lock phase. Call only after Task and Clue are locked.
     * TemporaryFile rows are acquired by the mapper in id ASC order before Animal/HealthRecord writes.
     */
    public PreparedBindings lockAndValidate(Long ownerId, List<String> tokens) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Transaction synchronization is required for animal file binding");
        }
        List<BoundFile> bound = new ArrayList<>();
        List<String> createdFormalPaths = new ArrayList<>();
        Map<String, TemporaryFile> byToken = new HashMap<>();
        PreparedBindings prepared = new PreparedBindings(byToken, bound, createdFormalPaths);
        registerSynchronization(prepared);
        if (tokens == null || tokens.isEmpty()) return prepared;

        List<TemporaryFile> locked = temporaryFileMapper.selectForUpdateByTokens(tokens);
        if (locked.size() != tokens.size()) throw new BusinessException(ErrorCode.INVALID_FILE);
        LocalDateTime now = LocalDateTime.now(TimeUtils.ZONE);
        for (TemporaryFile file : locked) {
            validate(file, ownerId, now);
            byToken.put(file.getToken(), file);
        }
        if (byToken.size() != tokens.size()) throw new BusinessException(ErrorCode.INVALID_FILE);
        return prepared;
    }

    /**
     * Frozen SUCCESS write phase after Animal IDs exist. Image order follows each Animal's client token order.
     */
    public void bindPrepared(Long ownerId, PreparedBindings prepared, List<AnimalTokens> animalTokens) {
        LocalDateTime now = LocalDateTime.now(TimeUtils.ZONE);
        for (AnimalTokens item : animalTokens) {
            int sortOrder=1;
            for (String token : item.tokens) {
                TemporaryFile temp=prepared.byToken.get(token);
                if (temp == null) throw new BusinessException(ErrorCode.INVALID_FILE);
                String formalPath=buildFormalPath(item.animalId, temp.getFileExtension());
                AnimalImage image=new AnimalImage();
                image.setAnimalId(item.animalId); image.setImagePath(formalPath); image.setSortOrder(sortOrder++);
                if (imageMapper.insert(image) != 1) throw new IllegalStateException("animal_image insert affected rows != 1");
                int rows=temporaryFileMapper.bindUploaded(temp.getId(), ownerId, "ANIMAL", item.animalId, formalPath, now);
                if (rows != 1) throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT, "临时文件已失效或被其他业务使用");
                prepared.boundFiles.add(new BoundFile(temp.getId(), temp.getTempPath(), item.animalId, formalPath));
            }
        }
    }

    /** M04 append-image write phase. Caller has already locked the parent Animal, plain-read Task, then locked temp rows. */
    public void bindPreparedStartingAt(Long ownerId, PreparedBindings prepared, Long animalId, List<String> tokens, int startSortOrder) {
        if (startSortOrder < 1) throw new IllegalArgumentException("startSortOrder must be >= 1");
        LocalDateTime now = LocalDateTime.now(TimeUtils.ZONE);
        int sortOrder=startSortOrder;
        for (String token : tokens) {
            TemporaryFile temp=prepared.byToken.get(token);
            if (temp == null) throw new BusinessException(ErrorCode.INVALID_FILE);
            String formalPath=buildFormalPath(animalId, temp.getFileExtension());
            AnimalImage image=new AnimalImage();
            image.setAnimalId(animalId); image.setImagePath(formalPath); image.setSortOrder(sortOrder++);
            if (imageMapper.insert(image) != 1) throw new IllegalStateException("animal_image insert affected rows != 1");
            int rows=temporaryFileMapper.bindUploaded(temp.getId(), ownerId, "ANIMAL", animalId, formalPath, now);
            if (rows != 1) throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT, "临时文件已失效或被其他业务使用");
            prepared.boundFiles.add(new BoundFile(temp.getId(), temp.getTempPath(), animalId, formalPath));
        }
    }

    public void copyPrepared(PreparedBindings prepared) {
        for (BoundFile file : prepared.boundFiles) {
            storage.copyTemporaryToFormal(file.tempPath, file.formalPath);
            prepared.createdFormalPaths.add(file.formalPath);
        }
    }

    private void validate(TemporaryFile file, Long ownerId, LocalDateTime now) {
        if (file == null || !ownerId.equals(file.getOwnerId()) || !"UPLOADED".equals(file.getStatus())
                || file.getExpiresAt() == null || !file.getExpiresAt().isAfter(now)
                || file.getFileSizeBytes() == null || file.getFileSizeBytes() <= 0 || file.getFileSizeBytes() > MAX_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }
        String ext=file.getFileExtension(), mime=file.getMimeType();
        boolean ok=(("jpg".equals(ext)||"jpeg".equals(ext))&&"image/jpeg".equals(mime))
                || ("png".equals(ext)&&"image/png".equals(mime));
        if (!ok) throw new BusinessException(ErrorCode.INVALID_FILE);
    }

    private String buildFormalPath(Long animalId, String extension) {
        return "animals/" + animalId + "/" + UUID.randomUUID().toString().toLowerCase(Locale.ROOT) + "." + extension;
    }

    private void registerSynchronization(PreparedBindings prepared) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                for (BoundFile file : prepared.boundFiles) {
                    try {
                        cleanupService.cleanupOne(file.temporaryFileId, file.tempPath, "ANIMAL", file.animalId, file.formalPath);
                    } catch (RuntimeException ex) {
                        log.error("Post-commit animal temporary cleanup callback failed: tempId={}", file.temporaryFileId, ex);
                    }
                }
            }
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    for (String path : prepared.createdFormalPaths) storage.deleteQuietly(path, "animal creation transaction rollback");
                }
            }
        });
    }

    public static class AnimalTokens {
        private final Long animalId; private final List<String> tokens;
        public AnimalTokens(Long animalId, List<String> tokens){ this.animalId=animalId; this.tokens=List.copyOf(tokens); }
        public Long getAnimalId(){ return animalId; }
        public List<String> getTokens(){ return tokens; }
    }
    public static class PreparedBindings {
        private final Map<String, TemporaryFile> byToken;
        private final List<BoundFile> boundFiles;
        private final List<String> createdFormalPaths;
        private PreparedBindings(Map<String, TemporaryFile> byToken, List<BoundFile> boundFiles, List<String> createdFormalPaths){
            this.byToken=byToken; this.boundFiles=boundFiles; this.createdFormalPaths=createdFormalPaths;
        }
    }
    private static class BoundFile {
        private final Long temporaryFileId; private final String tempPath; private final Long animalId; private final String formalPath;
        private BoundFile(Long temporaryFileId, String tempPath, Long animalId, String formalPath){
            this.temporaryFileId=temporaryFileId; this.tempPath=tempPath; this.animalId=animalId; this.formalPath=formalPath;
        }
    }
}

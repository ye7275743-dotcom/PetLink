package com.petlink.infrastructure.file.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.common.TimeUtils;
import com.petlink.infrastructure.file.dto.TemporaryUploadResponse;
import com.petlink.infrastructure.file.entity.TemporaryFile;
import com.petlink.infrastructure.file.mapper.TemporaryFileMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class TemporaryFileService {
    private static final Logger log = LoggerFactory.getLogger(TemporaryFileService.class);
    private static final long MAX_SIZE = 5L * 1024L * 1024L;

    private final TemporaryFileMapper mapper;
    private final FileProperties properties;

    public TemporaryFileService(TemporaryFileMapper mapper, FileProperties properties) {
        this.mapper = mapper;
        this.properties = properties;
    }

    @Transactional
    public TemporaryUploadResponse upload(Long ownerId, MultipartFile file) {
        ValidatedImage image = validate(file);
        String token = UUID.randomUUID().toString().toLowerCase(Locale.ROOT);
        String relative = "temporary/" + ownerId + "/" + token + "." + image.extension;
        Path root = validatedRoot();
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }

        LocalDateTime expiresAt = LocalDateTime.now(TimeUtils.ZONE).plus(properties.getTemporaryTtl());
        boolean physicalCreated = false;
        try {
            Files.createDirectories(target.getParent());
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            physicalCreated = true;

            // Method-level catch cannot observe a proxy-level transaction COMMIT failure.
            // Register rollback cleanup while the transaction synchronization is active.
            registerRollbackCleanup(target);

            TemporaryFile record = new TemporaryFile();
            record.setToken(token);
            record.setOwnerId(ownerId);
            record.setTempPath(relative.replace('\\', '/'));
            record.setFileExtension(image.extension);
            record.setMimeType(image.mimeType);
            record.setFileSizeBytes(file.getSize());
            record.setStatus("UPLOADED");
            record.setExpiresAt(expiresAt);
            if (mapper.insert(record) != 1) {
                throw new IllegalStateException("temporary_file insert affected rows != 1");
            }
        } catch (Exception ex) {
            if (physicalCreated) {
                deleteQuietly(target, "immediate upload failure");
            }
            if (ex instanceof BusinessException) {
                throw (BusinessException) ex;
            }
            log.error("Temporary file save failed for ownerId={}", ownerId, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "临时文件保存失败");
        }
        return new TemporaryUploadResponse(token, TimeUtils.toOffset(expiresAt));
    }

    private void registerRollbackCleanup(Path target) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // This service is expected to be called through the Spring @Transactional proxy.
            // Failing closed here prevents a file from being written without rollback cleanup.
            deleteQuietly(target, "missing transaction synchronization");
            throw new IllegalStateException("Transaction synchronization is not active for temporary upload");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    deleteQuietly(target, "transaction rollback/commit failure");
                }
            }
        });
    }

    private Path validatedRoot() {
        Path root = Path.of(properties.getRoot()).toAbsolutePath().normalize();
        if (root.getParent() == null) {
            throw new IllegalStateException("petlink.file.root must not resolve to a filesystem root");
        }
        return root;
    }

    private void deleteQuietly(Path path, String reason) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException cleanupEx) {
            log.error("Failed to cleanup temporary physical file after {}: {}", reason, path, cleanupEx);
        }
    }

    private ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        String original = file.getOriginalFilename();
        if (original == null || !original.contains(".")) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }
        String extension = original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!(extension.equals("jpg") || extension.equals("jpeg") || extension.equals("png"))) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }

        String declaredMime = file.getContentType();
        String expectedMime = extension.equals("png") ? "image/png" : "image/jpeg";
        if (!expectedMime.equals(declaredMime)) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }

        byte[] header = new byte[8];
        int read;
        try (InputStream input = file.getInputStream()) {
            read = input.read(header);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }

        boolean signatureOk;
        if (extension.equals("png")) {
            signatureOk = read >= 8
                    && (header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47
                    && header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A;
        } else {
            signatureOk = read >= 3
                    && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
        }
        if (!signatureOk) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }

        return new ValidatedImage(extension, expectedMime);
    }

    private static class ValidatedImage {
        private final String extension;
        private final String mimeType;

        private ValidatedImage(String extension, String mimeType) {
            this.extension = extension;
            this.mimeType = mimeType;
        }
    }
}

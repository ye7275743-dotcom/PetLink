package com.petlink.infrastructure.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.petlink.common.TimeUtils;
import com.petlink.infrastructure.file.entity.TemporaryFile;
import com.petlink.infrastructure.file.mapper.TemporaryFileMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class TemporaryFileCleanupJob {
    private static final Logger log = LoggerFactory.getLogger(TemporaryFileCleanupJob.class);

    private final TemporaryFileMapper mapper;
    private final FileProperties properties;

    public TemporaryFileCleanupJob(TemporaryFileMapper mapper, FileProperties properties) {
        this.mapper = mapper;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${petlink.file.cleanup-interval-ms:3600000}")
    public void cleanupExpiredUploadedFiles() {
        LocalDateTime now = LocalDateTime.now(TimeUtils.ZONE);
        List<TemporaryFile> expired = mapper.selectList(new LambdaQueryWrapper<TemporaryFile>()
                .eq(TemporaryFile::getStatus, "UPLOADED")
                .lt(TemporaryFile::getExpiresAt, now)
                .orderByAsc(TemporaryFile::getId)
                .last("LIMIT 200"));

        Path root = Path.of(properties.getRoot()).toAbsolutePath().normalize();
        for (TemporaryFile record : expired) {
            Path target = root.resolve(record.getTempPath()).normalize();
            if (!target.startsWith(root)) {
                log.error("Skip unsafe temporary_file path, id={}, path={}", record.getId(), record.getTempPath());
                continue;
            }
            try {
                Files.deleteIfExists(target);
                mapper.deleteById(record.getId());
            } catch (IOException ex) {
                // Preserve the row for retry/manual inspection when physical deletion fails.
                log.error("Failed to delete expired temporary file, id={}, path={}", record.getId(), target, ex);
            } catch (RuntimeException ex) {
                // A database failure for one row must not abort cleanup of the remaining batch.
                // If the physical file was already deleted, the retained row will be retried later.
                log.error("Failed to delete expired temporary_file row, id={}; continuing batch", record.getId(), ex);
            }
        }
    }
}

package com.petlink.infrastructure.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.petlink.infrastructure.file.entity.TemporaryFile;
import com.petlink.infrastructure.file.mapper.TemporaryFileMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/** Retries post-commit BOUND cleanup without weakening the Frozen safety checks. */
@Component
public class BoundTemporaryFileCleanupJob {
    private static final Logger log = LoggerFactory.getLogger(BoundTemporaryFileCleanupJob.class);
    private static final int BATCH_SIZE = 200;

    private final TemporaryFileMapper mapper;
    private final BoundTemporaryFileCleanupService cleanupService;

    public BoundTemporaryFileCleanupJob(TemporaryFileMapper mapper,
                                        BoundTemporaryFileCleanupService cleanupService) {
        this.mapper = mapper;
        this.cleanupService = cleanupService;
    }

    @Scheduled(fixedDelayString = "${petlink.file.cleanup-interval-ms:3600000}")
    public void retryResidualBoundFiles() {
        List<TemporaryFile> residual = mapper.selectList(new LambdaQueryWrapper<TemporaryFile>()
                .eq(TemporaryFile::getStatus, "BOUND")
                .orderByAsc(TemporaryFile::getId)
                .last("LIMIT " + BATCH_SIZE));
        for (TemporaryFile record : residual) {
            try {
                cleanupService.cleanupOne(record.getId(), record.getTempPath(), record.getBusinessType(),
                        record.getBusinessId(), record.getFormalPath());
            } catch (RuntimeException ex) {
                // A single inconsistent row must never prevent later residuals from being retried.
                log.error("Failed to retry BOUND temporary_file cleanup, id={}; continuing batch",
                        record.getId(), ex);
            }
        }
    }
}

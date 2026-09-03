package com.petlink.infrastructure.file.service;

import com.petlink.infrastructure.file.mapper.TemporaryFileMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoundTemporaryFileCleanupService {
    private static final Logger log = LoggerFactory.getLogger(BoundTemporaryFileCleanupService.class);

    private final TemporaryFileMapper mapper;
    private final FileStorageService storage;
    private final BoundFileAssociationVerifier associationVerifier;

    public BoundTemporaryFileCleanupService(TemporaryFileMapper mapper,
                                            FileStorageService storage,
                                            BoundFileAssociationVerifier associationVerifier) {
        this.mapper = mapper;
        this.storage = storage;
        this.associationVerifier = associationVerifier;
    }

    /**
     * Post-commit cleanup for a BOUND temporary file.
     * The technical record/temp original are removed only after both the formal DB association
     * and the formal physical file have been verified. Any inconsistency is preserved for manual inspection.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanupOne(Long temporaryFileId, String tempPath,
                           String businessType, Long businessId, String formalPath) {
        try {
            boolean associationExists = associationVerifier.exists(businessType, businessId, formalPath);
            boolean formalFileExists = storage.existsRegularFileInsideRoot(formalPath);
            if (!associationExists || !formalFileExists) {
                log.error("Preserving BOUND temporary file for manual inspection: id={}, business={}/{}, associationExists={}, formalFileExists={}",
                        temporaryFileId, businessType, businessId, associationExists, formalFileExists);
                return;
            }

            if (!storage.deleteIfExists(tempPath, "post-commit temporary cleanup")) {
                return;
            }
            int rows = mapper.deleteBoundRecord(temporaryFileId, businessType, businessId, formalPath);
            if (rows != 1) {
                log.warn("BOUND temporary_file cleanup did not delete exactly one row: id={}, rows={}", temporaryFileId, rows);
            }
        } catch (RuntimeException ex) {
            // Business transaction is already committed. Preserve residue and let a later cleanup/manual audit retry.
            log.error("Post-commit BOUND temporary cleanup failed: id={}, business={}/{}",
                    temporaryFileId, businessType, businessId, ex);
        }
    }
}

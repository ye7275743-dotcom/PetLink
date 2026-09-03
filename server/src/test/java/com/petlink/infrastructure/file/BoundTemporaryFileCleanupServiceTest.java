package com.petlink.infrastructure.file;

import com.petlink.infrastructure.file.mapper.TemporaryFileMapper;
import com.petlink.infrastructure.file.service.BoundFileAssociationVerifier;
import com.petlink.infrastructure.file.service.BoundTemporaryFileCleanupService;
import com.petlink.infrastructure.file.service.FileStorageService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class BoundTemporaryFileCleanupServiceTest {
    @Test
    void inconsistentFormalAssociationIsPreservedForManualInspection() {
        TemporaryFileMapper mapper = mock(TemporaryFileMapper.class);
        FileStorageService storage = mock(FileStorageService.class);
        BoundFileAssociationVerifier verifier = mock(BoundFileAssociationVerifier.class);
        BoundTemporaryFileCleanupService service = new BoundTemporaryFileCleanupService(mapper, storage, verifier);

        when(verifier.exists("RESCUE_CLUE", 3001L, "rescue-clues/3001/a.png")).thenReturn(false);
        when(storage.existsRegularFileInsideRoot("rescue-clues/3001/a.png")).thenReturn(true);

        service.cleanupOne(9L, "temporary/a.png", "RESCUE_CLUE", 3001L, "rescue-clues/3001/a.png");

        verify(storage, never()).deleteIfExists(eq("temporary/a.png"), anyString());
        verify(mapper, never()).deleteBoundRecord(anyLong(), anyString(), anyLong(), anyString());
    }

    @Test
    void verifiedFormalAssociationAllowsTempAndTechnicalRowCleanup() {
        TemporaryFileMapper mapper = mock(TemporaryFileMapper.class);
        FileStorageService storage = mock(FileStorageService.class);
        BoundFileAssociationVerifier verifier = mock(BoundFileAssociationVerifier.class);
        BoundTemporaryFileCleanupService service = new BoundTemporaryFileCleanupService(mapper, storage, verifier);

        when(verifier.exists("RESCUE_CLUE", 3001L, "rescue-clues/3001/a.png")).thenReturn(true);
        when(storage.existsRegularFileInsideRoot("rescue-clues/3001/a.png")).thenReturn(true);
        when(storage.deleteIfExists("temporary/a.png", "post-commit temporary cleanup")).thenReturn(true);
        when(mapper.deleteBoundRecord(9L, "RESCUE_CLUE", 3001L, "rescue-clues/3001/a.png")).thenReturn(1);

        service.cleanupOne(9L, "temporary/a.png", "RESCUE_CLUE", 3001L, "rescue-clues/3001/a.png");

        verify(storage).deleteIfExists("temporary/a.png", "post-commit temporary cleanup");
        verify(mapper).deleteBoundRecord(9L, "RESCUE_CLUE", 3001L, "rescue-clues/3001/a.png");
    }
}

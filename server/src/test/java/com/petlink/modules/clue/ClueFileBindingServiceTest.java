package com.petlink.modules.clue;

import com.petlink.infrastructure.file.entity.TemporaryFile;
import com.petlink.infrastructure.file.mapper.TemporaryFileMapper;
import com.petlink.infrastructure.file.service.BoundTemporaryFileCleanupService;
import com.petlink.infrastructure.file.service.FileStorageService;
import com.petlink.modules.clue.entity.RescueClueImage;
import com.petlink.modules.clue.mapper.RescueClueImageMapper;
import com.petlink.modules.clue.service.ClueFileBindingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ClueFileBindingServiceTest {
    private TemporaryFileMapper temporaryMapper;
    private RescueClueImageMapper imageMapper;
    private FileStorageService storage;
    private ClueFileBindingService service;

    @BeforeEach
    void setUp() {
        temporaryMapper = mock(TemporaryFileMapper.class);
        imageMapper = mock(RescueClueImageMapper.class);
        storage = mock(FileStorageService.class);
        BoundTemporaryFileCleanupService cleanup = mock(BoundTemporaryFileCleanupService.class);
        service = new ClueFileBindingService(temporaryMapper, imageMapper, storage, cleanup);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void locksByMapperOrderButAssignsSortOrderInClientTokenOrder() {
        String tokenA = "550e8400-e29b-41d4-a716-446655440001";
        String tokenB = "550e8400-e29b-41d4-a716-446655440002";
        TemporaryFile a = temp(1L, tokenA);
        TemporaryFile b = temp(2L, tokenB);
        when(temporaryMapper.selectForUpdateByTokens(List.of(tokenB, tokenA))).thenReturn(List.of(a, b));
        when(temporaryMapper.bindUploaded(anyLong(), eq(1001L), eq("RESCUE_CLUE"), eq(3001L), anyString(), any()))
                .thenReturn(1);
        when(imageMapper.insert(any())).thenReturn(1);

        service.bindImages(1001L, 3001L, List.of(tokenB, tokenA), 1);

        ArgumentCaptor<RescueClueImage> images = ArgumentCaptor.forClass(RescueClueImage.class);
        verify(imageMapper, times(2)).insert(images.capture());
        assertEquals(1, images.getAllValues().get(0).getSortOrder());
        assertEquals(2, images.getAllValues().get(1).getSortOrder());

        InOrder order = inOrder(temporaryMapper);
        order.verify(temporaryMapper).selectForUpdateByTokens(List.of(tokenB, tokenA));
        order.verify(temporaryMapper).bindUploaded(eq(2L), eq(1001L), eq("RESCUE_CLUE"), eq(3001L), anyString(), any());
        order.verify(temporaryMapper).bindUploaded(eq(1L), eq(1001L), eq("RESCUE_CLUE"), eq(3001L), anyString(), any());
    }

    @Test
    void prepareBindingDoesNotCopyUntilExplicitCopyPhase() {
        String token = "550e8400-e29b-41d4-a716-446655440003";
        TemporaryFile file = temp(3L, token);
        when(temporaryMapper.selectForUpdateByTokens(List.of(token))).thenReturn(List.of(file));
        when(temporaryMapper.bindUploaded(anyLong(), eq(1001L), eq("RESCUE_CLUE"), eq(3001L), anyString(), any()))
                .thenReturn(1);
        when(imageMapper.insert(any())).thenReturn(1);

        ClueFileBindingService.PreparedBindings prepared = service.prepareBindings(1001L, 3001L, List.of(token), 1);
        verify(storage, never()).copyTemporaryToFormal(anyString(), anyString());

        service.copyPrepared(prepared);
        verify(storage).copyTemporaryToFormal(eq(file.getTempPath()), startsWith("rescue-clues/3001/"));
    }

    private TemporaryFile temp(Long id, String token) {
        TemporaryFile file = new TemporaryFile();
        file.setId(id);
        file.setToken(token);
        file.setOwnerId(1001L);
        file.setTempPath("temporary/1001/" + token + ".jpg");
        file.setFileExtension("jpg");
        file.setMimeType("image/jpeg");
        file.setFileSizeBytes(100L);
        file.setStatus("UPLOADED");
        file.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        return file;
    }
}

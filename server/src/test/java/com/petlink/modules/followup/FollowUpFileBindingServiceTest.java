package com.petlink.modules.followup;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.infrastructure.file.entity.TemporaryFile;
import com.petlink.infrastructure.file.mapper.TemporaryFileMapper;
import com.petlink.infrastructure.file.service.BoundTemporaryFileCleanupService;
import com.petlink.infrastructure.file.service.FileStorageService;
import com.petlink.modules.followup.entity.FollowUpImage;
import com.petlink.modules.followup.mapper.FollowUpImageMapper;
import com.petlink.modules.followup.service.FollowUpFileBindingService;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FollowUpFileBindingServiceTest {
    private TemporaryFileMapper temporaryMapper;
    private FollowUpImageMapper imageMapper;
    private FileStorageService storage;
    private BoundTemporaryFileCleanupService cleanup;
    private FollowUpFileBindingService service;

    @BeforeEach
    void setUp() {
        temporaryMapper = mock(TemporaryFileMapper.class);
        imageMapper = mock(FollowUpImageMapper.class);
        storage = mock(FileStorageService.class);
        cleanup = mock(BoundTemporaryFileCleanupService.class);
        service = new FollowUpFileBindingService(temporaryMapper, imageMapper, storage, cleanup);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void temporaryFileMapperContractLocksRowsByIdAscending() throws Exception {
        Method method = TemporaryFileMapper.class.getMethod("selectForUpdateByTokens", List.class);
        Select select = method.getAnnotation(Select.class);
        assertNotNull(select);
        String sql = String.join(" ", select.value()).replaceAll("\\s+", " ");
        assertTrue(sql.contains("ORDER BY id ASC FOR UPDATE"));
    }

    @Test
    void lockedRowsMayBeIdOrderedButImagesKeepClientTokenOrder() {
        String jpgToken = "550e8400-e29b-41d4-a716-446655440041";
        String pngToken = "550e8400-e29b-41d4-a716-446655440042";
        TemporaryFile jpg = temp(41L, jpgToken, "jpg", "image/jpeg");
        TemporaryFile png = temp(42L, pngToken, "png", "image/png");
        List<String> clientOrder = List.of(pngToken, jpgToken);

        when(temporaryMapper.selectForUpdateByTokens(clientOrder)).thenReturn(List.of(jpg, png));
        when(imageMapper.insert(any())).thenReturn(1);
        when(temporaryMapper.bindUploaded(anyLong(), eq(1001L), eq("FOLLOW_UP"), eq(8001L), anyString(), any()))
                .thenReturn(1);

        FollowUpFileBindingService.PreparedBindings prepared = service.lockAndValidate(1001L, clientOrder);
        service.bindPrepared(1001L, 8001L, clientOrder, prepared);

        ArgumentCaptor<FollowUpImage> images = ArgumentCaptor.forClass(FollowUpImage.class);
        verify(imageMapper, times(2)).insert(images.capture());
        assertEquals(1, images.getAllValues().get(0).getSortOrder());
        assertEquals(2, images.getAllValues().get(1).getSortOrder());
        assertTrue(images.getAllValues().get(0).getImagePath().endsWith(".png"));
        assertTrue(images.getAllValues().get(1).getImagePath().endsWith(".jpg"));

        InOrder order = inOrder(temporaryMapper);
        order.verify(temporaryMapper).selectForUpdateByTokens(clientOrder);
        order.verify(temporaryMapper).bindUploaded(eq(42L), eq(1001L), eq("FOLLOW_UP"), eq(8001L), anyString(), any());
        order.verify(temporaryMapper).bindUploaded(eq(41L), eq(1001L), eq("FOLLOW_UP"), eq(8001L), anyString(), any());
    }

    @Test
    void zeroAffectedRowsOnBoundStopsBeforePhysicalCopy() {
        String token = "550e8400-e29b-41d4-a716-446655440043";
        TemporaryFile file = temp(43L, token, "png", "image/png");
        when(temporaryMapper.selectForUpdateByTokens(List.of(token))).thenReturn(List.of(file));
        when(imageMapper.insert(any())).thenReturn(1);
        when(temporaryMapper.bindUploaded(eq(43L), eq(1001L), eq("FOLLOW_UP"), eq(8001L), anyString(), any()))
                .thenReturn(0);

        FollowUpFileBindingService.PreparedBindings prepared = service.lockAndValidate(1001L, List.of(token));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.bindPrepared(1001L, 8001L, List.of(token), prepared));

        assertEquals(ErrorCode.BUSINESS_STATE_CONFLICT, ex.getErrorCode());
        verifyNoInteractions(storage);
        verifyNoInteractions(cleanup);
    }

    @Test
    void copyFailureRegistersFormalPathForRollbackAndPreservesTemporaryOriginal() {
        String token = "550e8400-e29b-41d4-a716-446655440044";
        TemporaryFile file = temp(44L, token, "png", "image/png");
        when(temporaryMapper.selectForUpdateByTokens(List.of(token))).thenReturn(List.of(file));
        when(imageMapper.insert(any())).thenReturn(1);
        when(temporaryMapper.bindUploaded(eq(44L), eq(1001L), eq("FOLLOW_UP"), eq(8001L), anyString(), any()))
                .thenReturn(1);

        FollowUpFileBindingService.PreparedBindings prepared = service.lockAndValidate(1001L, List.of(token));
        service.bindPrepared(1001L, 8001L, List.of(token), prepared);
        ArgumentCaptor<FollowUpImage> image = ArgumentCaptor.forClass(FollowUpImage.class);
        verify(imageMapper).insert(image.capture());
        String formalPath = image.getValue().getImagePath();
        doThrow(new BusinessException(ErrorCode.INTERNAL_ERROR, "copy failed"))
                .when(storage).copyTemporaryToFormal(file.getTempPath(), formalPath);

        assertThrows(BusinessException.class, () -> service.copyPrepared(prepared));
        complete(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(storage).deleteQuietly(formalPath, "follow-up creation transaction rollback");
        verify(cleanup, never()).cleanupOne(anyLong(), anyString(), anyString(), anyLong(), anyString());
        verify(temporaryMapper, never()).deleteBoundRecord(anyLong(), anyString(), anyLong(), anyString());
    }

    @Test
    void temporaryCleanupRunsOnlyAfterCommitAndClientSortOrderIsPersisted() {
        String token = "550e8400-e29b-41d4-a716-446655440045";
        TemporaryFile file = temp(45L, token, "jpg", "image/jpeg");
        when(temporaryMapper.selectForUpdateByTokens(List.of(token))).thenReturn(List.of(file));
        when(imageMapper.insert(any())).thenReturn(1);
        when(temporaryMapper.bindUploaded(eq(45L), eq(1001L), eq("FOLLOW_UP"), eq(8001L), anyString(), any()))
                .thenReturn(1);

        FollowUpFileBindingService.PreparedBindings prepared = service.lockAndValidate(1001L, List.of(token));
        service.bindPrepared(1001L, 8001L, List.of(token), prepared);
        ArgumentCaptor<FollowUpImage> image = ArgumentCaptor.forClass(FollowUpImage.class);
        verify(imageMapper).insert(image.capture());
        String formalPath = image.getValue().getImagePath();
        assertEquals(1, image.getValue().getSortOrder());

        service.copyPrepared(prepared);
        verifyNoInteractions(cleanup);
        commit();

        verify(cleanup).cleanupOne(45L, file.getTempPath(), "FOLLOW_UP", 8001L, formalPath);
        verify(storage, never()).deleteQuietly(formalPath, "follow-up creation transaction rollback");
    }

    private void commit() {
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }
        complete(TransactionSynchronization.STATUS_COMMITTED);
    }

    private void complete(int status) {
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCompletion(status);
        }
    }

    private TemporaryFile temp(Long id, String token, String extension, String mime) {
        TemporaryFile file = new TemporaryFile();
        file.setId(id);
        file.setToken(token);
        file.setOwnerId(1001L);
        file.setTempPath("temporary/1001/" + token + "." + extension);
        file.setFileExtension(extension);
        file.setMimeType(mime);
        file.setFileSizeBytes(100L);
        file.setStatus("UPLOADED");
        file.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        return file;
    }
}

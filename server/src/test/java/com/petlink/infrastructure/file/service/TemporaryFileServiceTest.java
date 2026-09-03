package com.petlink.infrastructure.file.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.infrastructure.file.mapper.TemporaryFileMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TemporaryFileServiceTest {
    @TempDir Path tempDir;

    @AfterEach
    void cleanupSync() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void rollbackCompletionDeletesPhysicalFile() throws Exception {
        TemporaryFileMapper mapper = mock(TemporaryFileMapper.class);
        when(mapper.insert(any())).thenReturn(1);
        FileProperties properties = new FileProperties();
        properties.setRoot(tempDir.toString());
        TemporaryFileService service = new TemporaryFileService(mapper, properties);

        byte[] png = new byte[] {(byte)0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A,1};
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", png);
        TransactionSynchronizationManager.initSynchronization();
        service.upload(1L, file);
        Path created = Files.walk(tempDir).filter(Files::isRegularFile).findFirst().orElseThrow();
        assertTrue(Files.exists(created));

        for (TransactionSynchronization s : TransactionSynchronizationManager.getSynchronizations()) {
            s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }
        assertFalse(Files.exists(created));
    }

    @Test
    void wrongMimeIsRejectedBeforeWrite() {
        TemporaryFileService service = new TemporaryFileService(mock(TemporaryFileMapper.class), props());
        byte[] png = new byte[] {(byte)0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A,1};
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/jpeg", png);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.upload(1L, file));
        assertEquals(ErrorCode.INVALID_FILE, ex.getErrorCode());
    }

    @Test
    void wrongFileHeaderIsRejectedBeforeWrite() {
        TemporaryFileService service = new TemporaryFileService(mock(TemporaryFileMapper.class), props());
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", "not-jpeg".getBytes());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.upload(1L, file));
        assertEquals(ErrorCode.INVALID_FILE, ex.getErrorCode());
    }

    private FileProperties props() {
        FileProperties p = new FileProperties();
        p.setRoot(tempDir.toString());
        return p;
    }

    @Test
    void fileOverFiveMbIsRejected() {
        TemporaryFileService service = new TemporaryFileService(mock(TemporaryFileMapper.class), props());
        byte[] bytes = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", bytes);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.upload(1L, file));
        assertEquals(ErrorCode.FILE_TOO_LARGE, ex.getErrorCode());
    }
}

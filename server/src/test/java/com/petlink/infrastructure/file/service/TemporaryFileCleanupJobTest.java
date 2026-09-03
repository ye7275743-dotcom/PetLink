package com.petlink.infrastructure.file.service;

import com.petlink.infrastructure.file.entity.TemporaryFile;
import com.petlink.infrastructure.file.mapper.TemporaryFileMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TemporaryFileCleanupJobTest {
    @TempDir Path tempDir;

    @Test
    void databaseFailureOnOneRowDoesNotAbortRemainingBatch() throws Exception {
        TemporaryFileMapper mapper = mock(TemporaryFileMapper.class);
        FileProperties properties = new FileProperties();
        properties.setRoot(tempDir.toString());

        TemporaryFile first = expired(1L, "temporary/1.png");
        TemporaryFile second = expired(2L, "temporary/2.png");
        when(mapper.selectList(any())).thenReturn(List.of(first, second));
        doThrow(new RuntimeException("simulated database failure"))
                .when(mapper).deleteById(1L);
        when(mapper.deleteById(2L)).thenReturn(1);

        Path firstPath = tempDir.resolve(first.getTempPath());
        Path secondPath = tempDir.resolve(second.getTempPath());
        Files.createDirectories(firstPath.getParent());
        Files.write(firstPath, new byte[]{1});
        Files.write(secondPath, new byte[]{2});

        new TemporaryFileCleanupJob(mapper, properties).cleanupExpiredUploadedFiles();

        verify(mapper).deleteById(1L);
        verify(mapper).deleteById(2L);
        assertFalse(Files.exists(secondPath));
    }

    private TemporaryFile expired(Long id, String tempPath) {
        TemporaryFile file = new TemporaryFile();
        file.setId(id);
        file.setStatus("UPLOADED");
        file.setTempPath(tempPath);
        file.setExpiresAt(LocalDateTime.now().minusHours(2));
        return file;
    }
}

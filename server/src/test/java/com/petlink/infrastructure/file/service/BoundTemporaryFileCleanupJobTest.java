package com.petlink.infrastructure.file.service;

import com.petlink.infrastructure.file.entity.TemporaryFile;
import com.petlink.infrastructure.file.mapper.TemporaryFileMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BoundTemporaryFileCleanupJobTest {
    @Test
    void oneResidualFailureDoesNotBlockLaterRows() {
        TemporaryFileMapper mapper = mock(TemporaryFileMapper.class);
        BoundTemporaryFileCleanupService cleanup = mock(BoundTemporaryFileCleanupService.class);
        TemporaryFile first = residual(1L);
        TemporaryFile second = residual(2L);
        when(mapper.selectList(any())).thenReturn(List.of(first, second));
        doThrow(new RuntimeException("simulated failure")).when(cleanup)
                .cleanupOne(1L, "temporary/1/a.png", "ANIMAL", 1L, "animals/1/a.png");

        new BoundTemporaryFileCleanupJob(mapper, cleanup).retryResidualBoundFiles();

        verify(cleanup).cleanupOne(1L, "temporary/1/a.png", "ANIMAL", 1L, "animals/1/a.png");
        verify(cleanup).cleanupOne(2L, "temporary/2/b.png", "ANIMAL", 2L, "animals/2/b.png");
    }

    private TemporaryFile residual(Long id) {
        TemporaryFile file = new TemporaryFile();
        file.setId(id);
        file.setStatus("BOUND");
        file.setBusinessType("ANIMAL");
        file.setBusinessId(id);
        file.setTempPath("temporary/" + id + "/" + (id == 1 ? "a.png" : "b.png"));
        file.setFormalPath("animals/" + id + "/" + (id == 1 ? "a.png" : "b.png"));
        return file;
    }
}

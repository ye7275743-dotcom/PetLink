package com.petlink.infrastructure.file.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class FormalFileOrphanScannerTest {
    @TempDir Path tempDir;

    @Test
    void deletesOnlyOldRecognizedUnassociatedFormalFiles() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        FileProperties properties = new FileProperties();
        properties.setRoot(tempDir.toString());
        FileStorageService storage = new FileStorageService(properties);
        FormalFileOrphanScanner scanner = new FormalFileOrphanScanner(jdbc, properties, storage);

        Path oldOrphan = create("animals/7/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa.png");
        Path recentOrphan = create("animals/7/bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb.png");
        Path associated = create("animals/7/cccccccc-cccc-cccc-cccc-cccccccccccc.png");
        Files.setLastModifiedTime(oldOrphan, FileTime.from(Instant.now().minus(Duration.ofHours(25))));
        when(jdbc.queryForList(anyString(), eq(String.class))).thenReturn(
                List.of("animals/7/cccccccc-cccc-cccc-cccc-cccccccccccc.png"));

        scanner.scanFormalFiles();

        assertFalse(Files.exists(oldOrphan));
        assertTrue(Files.exists(recentOrphan));
        assertTrue(Files.exists(associated));
    }

    @Test
    void preservesAllFilesWhenAssociationSnapshotFails() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        FileProperties properties = new FileProperties();
        properties.setRoot(tempDir.toString());
        FileStorageService storage = mock(FileStorageService.class);
        FormalFileOrphanScanner scanner = new FormalFileOrphanScanner(jdbc, properties, storage);
        Path oldOrphan = create("follow-ups/8/dddddddd-dddd-dddd-dddd-dddddddddddd.jpg");
        Files.setLastModifiedTime(oldOrphan, FileTime.from(Instant.now().minus(Duration.ofHours(25))));
        when(jdbc.queryForList(anyString(), eq(String.class))).thenThrow(new RuntimeException("database unavailable"));

        scanner.scanFormalFiles();

        assertTrue(Files.exists(oldOrphan));
        verifyNoInteractions(storage);
    }

    private Path create(String relative) throws Exception {
        Path file = tempDir.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[]{1, 2, 3});
        return file;
    }
}

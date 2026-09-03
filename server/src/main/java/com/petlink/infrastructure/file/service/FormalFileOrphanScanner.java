package com.petlink.infrastructure.file.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Removes only old, well-formed formal upload files with no committed business association.
 * Any uncertain database, path, symlink, or file-age condition is preserved and logged.
 */
@Component
public class FormalFileOrphanScanner {
    private static final Logger log = LoggerFactory.getLogger(FormalFileOrphanScanner.class);
    private static final Duration SAFETY_WINDOW = Duration.ofHours(24);
    private static final String ASSOCIATED_PATHS_SQL = """
            SELECT image_path FROM rescue_clue_image
            UNION ALL SELECT image_path FROM animal_image
            UNION ALL SELECT image_path FROM follow_up_image
            UNION ALL SELECT formal_path FROM temporary_file WHERE status='BOUND' AND formal_path IS NOT NULL
            """;

    private final JdbcTemplate jdbcTemplate;
    private final FileProperties properties;
    private final FileStorageService storage;

    public FormalFileOrphanScanner(JdbcTemplate jdbcTemplate, FileProperties properties,
                                   FileStorageService storage) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.storage = storage;
    }

    @Scheduled(fixedDelayString = "${petlink.file.cleanup-interval-ms:3600000}")
    public void scanFormalFiles() {
        Set<String> associated;
        try {
            associated = new HashSet<>(jdbcTemplate.queryForList(ASSOCIATED_PATHS_SQL, String.class));
        } catch (RuntimeException ex) {
            // Never delete when the association snapshot is incomplete or unavailable.
            log.error("Formal-file orphan scan skipped because association query failed", ex);
            return;
        }

        Path root;
        try {
            root = validatedRoot();
        } catch (RuntimeException ex) {
            log.error("Formal-file orphan scan skipped because upload root is invalid", ex);
            return;
        }
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) return;

        Instant cutoff = Instant.now().minus(SAFETY_WINDOW);
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .forEach(path -> inspectFormalFile(root, path, associated, cutoff));
        } catch (IOException ex) {
            log.error("Formal-file orphan scan could not walk upload root", ex);
        }
    }

    private void inspectFormalFile(Path root, Path path, Set<String> associated, Instant cutoff) {
        String relative;
        try {
            relative = root.relativize(path).toString().replace('\\', '/');
        } catch (RuntimeException ex) {
            log.warn("Preserving file with unresolvable relative path: {}", path, ex);
            return;
        }
        if (!isRecognizedFormalImage(relative) || associated.contains(relative)) return;

        try {
            FileTime modified = Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS);
            if (modified.toInstant().isAfter(cutoff)) {
                log.info("Preserving recent formal-file orphan within 24-hour safety window: {}", relative);
                return;
            }
        } catch (IOException ex) {
            log.warn("Preserving formal-file orphan whose age cannot be verified: {}", relative, ex);
            return;
        }

        if (storage.deleteIfExists(relative, "formal orphan scan")) {
            log.warn("Deleted formal-file orphan after 24-hour safety window: {}", relative);
        } else {
            log.error("Preserved formal-file orphan because safe deletion failed: {}", relative);
        }
    }

    private boolean isRecognizedFormalImage(String relative) {
        return relative.matches("(?:rescue-clues|animals|follow-ups)/[1-9][0-9]*/[0-9a-fA-F-]+\\.(?:jpg|jpeg|png)");
    }

    private Path validatedRoot() {
        Path root = Path.of(properties.getRoot()).toAbsolutePath().normalize();
        if (root.getParent() == null) throw new IllegalStateException("upload root must not be filesystem root");
        return root;
    }
}

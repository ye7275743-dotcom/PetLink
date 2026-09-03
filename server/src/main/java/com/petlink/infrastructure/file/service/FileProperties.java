package com.petlink.infrastructure.file.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "petlink.file")
public class FileProperties {
    @NotNull
    private Duration temporaryTtl = Duration.ofHours(1);

    @NotBlank
    private String root = "./data/uploads";

    private long cleanupIntervalMs = 3600000L;

    @AssertTrue(message = "petlink.file.temporary-ttl must be positive")
    public boolean isTemporaryTtlValid() {
        return temporaryTtl != null && !temporaryTtl.isZero() && !temporaryTtl.isNegative();
    }

    @AssertTrue(message = "petlink.file.cleanup-interval-ms must be positive")
    public boolean isCleanupIntervalValid() {
        return cleanupIntervalMs > 0;
    }

    @AssertTrue(message = "petlink.file.root must be a valid non-root filesystem path")
    public boolean isRootValid() {
        if (root == null || root.trim().isEmpty()) return false;
        try {
            java.nio.file.Path path = java.nio.file.Path.of(root).toAbsolutePath().normalize();
            return path.getParent() != null;
        } catch (java.nio.file.InvalidPathException ex) {
            return false;
        }
    }

    public Duration getTemporaryTtl() { return temporaryTtl; }
    public void setTemporaryTtl(Duration temporaryTtl) { this.temporaryTtl = temporaryTtl; }
    public String getRoot() { return root; }
    public void setRoot(String root) { this.root = root; }
    public long getCleanupIntervalMs() { return cleanupIntervalMs; }
    public void setCleanupIntervalMs(long cleanupIntervalMs) { this.cleanupIntervalMs = cleanupIntervalMs; }
}

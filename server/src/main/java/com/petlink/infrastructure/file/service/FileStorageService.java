package com.petlink.infrastructure.file.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private final FileProperties properties;

    public FileStorageService(FileProperties properties) {
        this.properties = properties;
    }

    public void copyTemporaryToFormal(String tempRelative, String formalRelative) {
        Path root = ensureRoot();
        Path source = resolveExistingInsideRoot(root, tempRelative);
        Path target = resolveTargetInsideRoot(root, formalRelative);
        try {
            Files.createDirectories(target.getParent());
            Path realParent = target.getParent().toRealPath();
            Path realRoot = root.toRealPath();
            if (!realParent.startsWith(realRoot)) {
                throw new BusinessException(ErrorCode.INVALID_FILE);
            }
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            deleteQuietly(formalRelative, "failed formal copy");
            log.error("Failed to copy temporary file to formal location: {} -> {}", tempRelative, formalRelative, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "正式文件保存失败");
        }
    }

    public ImageBinary readImage(String relative) {
        Path root = ensureRoot();
        Path path;
        try {
            path = resolveExistingInsideRoot(root, relative);
        } catch (BusinessException ex) {
            log.error("Stored image is missing or escapes uploads root: {}", relative);
            throw ex;
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            String contentType = detectImageType(bytes);
            if (contentType == null) {
                log.error("Stored image has invalid signature: {}", relative);
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            }
            return new ImageBinary(bytes, contentType);
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            log.error("Stored image cannot be read: {}", relative, ex);
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    public boolean existsRegularFileInsideRoot(String relative) {
        Path root = ensureRoot();
        try {
            resolveExistingInsideRoot(root, relative);
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }

    public boolean deleteIfExists(String relative, String reason) {
        Path root = ensureRoot();
        Path target;
        try {
            target = root.resolve(relative).normalize();
        } catch (RuntimeException ex) {
            log.error("Refusing invalid stored path during {}: {}", reason, relative, ex);
            return false;
        }
        if (!target.startsWith(root)) {
            log.error("Refusing path escape during {}: {}", reason, relative);
            return false;
        }
        try {
            if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                return true;
            }
            Path realRoot = root.toRealPath();
            Path realTarget = target.toRealPath();
            if (!realTarget.startsWith(realRoot) || Files.isSymbolicLink(target)) {
                log.error("Refusing path/symlink escape during {}: {}", reason, relative);
                return false;
            }
            Files.deleteIfExists(realTarget);
            return true;
        } catch (IOException ex) {
            log.error("Failed to delete stored file during {}: {}", reason, relative, ex);
            return false;
        }
    }

    public void deleteQuietly(String relative, String reason) {
        deleteIfExists(relative, reason);
    }

    private Path ensureRoot() {
        Path root = Path.of(properties.getRoot()).toAbsolutePath().normalize();
        if (root.getParent() == null) {
            throw new IllegalStateException("petlink.file.root must not resolve to a filesystem root");
        }
        try {
            Files.createDirectories(root);
            return root;
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot create petlink.file.root", ex);
        }
    }

    private Path resolveExistingInsideRoot(Path root, String relative) {
        if (relative == null || relative.isBlank()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        Path candidate = root.resolve(relative).normalize();
        if (!candidate.startsWith(root)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        try {
            Path realRoot = root.toRealPath();
            Path real = candidate.toRealPath();
            if (!real.startsWith(realRoot) || !Files.isRegularFile(real, LinkOption.NOFOLLOW_LINKS)) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            }
            return real;
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    private Path resolveTargetInsideRoot(Path root, String relative) {
        if (relative == null || relative.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }
        return target;
    }

    private String detectImageType(byte[] bytes) {
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
                && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) {
            return "image/png";
        }
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        return null;
    }

    public static class ImageBinary {
        private final byte[] bytes;
        private final String contentType;

        public ImageBinary(byte[] bytes, String contentType) {
            this.bytes = bytes;
            this.contentType = contentType;
        }

        public byte[] getBytes() { return bytes; }
        public String getContentType() { return contentType; }
    }
}

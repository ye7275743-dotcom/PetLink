package com.petlink.modules.clue.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.common.TimeUtils;
import com.petlink.modules.clue.dto.CreateClueRequest;
import com.petlink.modules.clue.dto.UpdateClueRequest;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
public class ClueRequestNormalizer {
    private static final Duration CLOCK_SKEW_TOLERANCE=Duration.ofMinutes(5);

    public NormalizedCreate normalizeCreate(Long userId, CreateClueRequest request) {
        if (request == null) throw invalid();
        String location = requiredText(request.getLocation(), 255);
        OffsetDateTime foundTime = requiredFoundTime(request.getFoundTime());
        String animalDescription = requiredText(request.getAnimalDescription(), 1000);
        String sceneDescription = optionalText(request.getSceneDescription(), 1000);
        String contact = requiredText(request.getContact(), 100);
        List<String> tokens = validateTokens(request.getImageTokens(), 1, 9);
        String fingerprint = fingerprint(userId, location, foundTime, animalDescription, sceneDescription, contact, tokens);
        return new NormalizedCreate(location, toDbTime(foundTime), animalDescription, sceneDescription, contact, tokens, fingerprint);
    }

    public NormalizedUpdate normalizeUpdate(UpdateClueRequest request) {
        if (request == null || !request.hasAnyField()) throw invalid();
        NormalizedUpdate out = new NormalizedUpdate();
        if (request.isLocationPresent()) {
            out.locationPresent = true;
            out.location = requiredText(request.getLocation(), 255);
        }
        if (request.isFoundTimePresent()) {
            out.foundTimePresent = true;
            out.foundTime = toDbTime(requiredFoundTime(request.getFoundTime()));
        }
        if (request.isAnimalDescriptionPresent()) {
            out.animalDescriptionPresent = true;
            out.animalDescription = requiredText(request.getAnimalDescription(), 1000);
        }
        if (request.isSceneDescriptionPresent()) {
            out.sceneDescriptionPresent = true;
            out.sceneDescription = optionalText(request.getSceneDescription(), 1000);
        }
        if (request.isContactPresent()) {
            out.contactPresent = true;
            out.contact = requiredText(request.getContact(), 100);
        }
        return out;
    }

    public List<String> normalizeImageTokens(List<String> tokens) {
        return validateTokens(tokens, 1, 9);
    }

    private OffsetDateTime requiredFoundTime(OffsetDateTime value) {
        if (value == null) throw invalid();
        // Browser and API hosts may differ by a few seconds. Keep the business rule
        // while tolerating normal distributed-system clock skew.
        if (value.toInstant().isAfter(Instant.now().plus(CLOCK_SKEW_TOLERANCE))) throw invalid();
        return value;
    }

    private LocalDateTime toDbTime(OffsetDateTime value) {
        return value.atZoneSameInstant(TimeUtils.ZONE).toLocalDateTime();
    }

    private String requiredText(String value, int max) {
        if (value == null) throw invalid();
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > max) throw invalid();
        return normalized;
    }

    private String optionalText(String value, int max) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.isEmpty()) return null;
        if (normalized.length() > max) throw invalid();
        return normalized;
    }

    private List<String> validateTokens(List<String> raw, int min, int max) {
        if (raw == null || raw.size() < min || raw.size() > max) throw invalid();
        List<String> result = new ArrayList<>(raw.size());
        Set<String> seen = new HashSet<>();
        for (String token : raw) {
            if (token == null) throw invalid();
            String normalized = token.trim().toLowerCase(Locale.ROOT);
            try {
                if (!UUID.fromString(normalized).toString().equals(normalized)) throw new IllegalArgumentException();
            } catch (IllegalArgumentException ex) {
                throw invalid();
            }
            if (!seen.add(normalized)) throw invalid();
            result.add(normalized);
        }
        return result;
    }

    private String fingerprint(Long userId, String location, OffsetDateTime foundTime,
                               String animalDescription, String sceneDescription, String contact,
                               List<String> tokens) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            updateLong(digest, userId);
            updateString(digest, location);
            updateString(digest, foundTime.toInstant().toString());
            updateString(digest, animalDescription);
            updateNullableString(digest, sceneDescription);
            updateString(digest, contact);
            updateLong(digest, (long) tokens.size());
            for (String token : tokens) updateString(digest, token);
            byte[] hash = digest.digest();
            StringBuilder out = new StringBuilder(hash.length * 2);
            for (byte b : hash) out.append(String.format("%02x", b));
            return out.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private void updateLong(MessageDigest digest, Long value) {
        digest.update(ByteBuffer.allocate(Long.BYTES).putLong(value).array());
    }

    private void updateNullableString(MessageDigest digest, String value) {
        if (value == null) {
            digest.update(ByteBuffer.allocate(4).putInt(-1).array());
        } else {
            updateString(digest, value);
        }
    }

    private void updateString(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(4).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private BusinessException invalid() { return new BusinessException(ErrorCode.INVALID_PARAMETER); }

    public static class NormalizedCreate {
        private final String location;
        private final LocalDateTime foundTime;
        private final String animalDescription;
        private final String sceneDescription;
        private final String contact;
        private final List<String> imageTokens;
        private final String fingerprint;

        public NormalizedCreate(String location, LocalDateTime foundTime, String animalDescription,
                                String sceneDescription, String contact, List<String> imageTokens,
                                String fingerprint) {
            this.location = location;
            this.foundTime = foundTime;
            this.animalDescription = animalDescription;
            this.sceneDescription = sceneDescription;
            this.contact = contact;
            this.imageTokens = imageTokens;
            this.fingerprint = fingerprint;
        }
        public String getLocation() { return location; }
        public LocalDateTime getFoundTime() { return foundTime; }
        public String getAnimalDescription() { return animalDescription; }
        public String getSceneDescription() { return sceneDescription; }
        public String getContact() { return contact; }
        public List<String> getImageTokens() { return imageTokens; }
        public String getFingerprint() { return fingerprint; }
    }

    public static class NormalizedUpdate {
        private boolean locationPresent;
        private String location;
        private boolean foundTimePresent;
        private LocalDateTime foundTime;
        private boolean animalDescriptionPresent;
        private String animalDescription;
        private boolean sceneDescriptionPresent;
        private String sceneDescription;
        private boolean contactPresent;
        private String contact;

        public boolean isLocationPresent() { return locationPresent; }
        public String getLocation() { return location; }
        public boolean isFoundTimePresent() { return foundTimePresent; }
        public LocalDateTime getFoundTime() { return foundTime; }
        public boolean isAnimalDescriptionPresent() { return animalDescriptionPresent; }
        public String getAnimalDescription() { return animalDescription; }
        public boolean isSceneDescriptionPresent() { return sceneDescriptionPresent; }
        public String getSceneDescription() { return sceneDescription; }
        public boolean isContactPresent() { return contactPresent; }
        public String getContact() { return contact; }
    }
}

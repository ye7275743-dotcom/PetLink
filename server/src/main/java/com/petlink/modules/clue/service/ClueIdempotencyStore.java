package com.petlink.modules.clue.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.common.TimeUtils;
import com.petlink.modules.clue.config.ClueApiProperties;
import com.petlink.modules.clue.vo.CreateClueResponse;
import com.petlink.modules.clue.vo.IdempotencyKeyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ClueIdempotencyStore {
    private static final Logger log = LoggerFactory.getLogger(ClueIdempotencyStore.class);

    enum State { UNUSED, PROCESSING, SUCCEEDED }

    private final ConcurrentHashMap<Scope, Entry> entries = new ConcurrentHashMap<>();
    private final ClueApiProperties properties;

    public ClueIdempotencyStore(ClueApiProperties properties) {
        this.properties = properties;
    }

    public IdempotencyKeyResponse issue(Long userId) {
        String key = UUID.randomUUID().toString().toLowerCase(Locale.ROOT);
        Instant expiresAt = Instant.now().plus(properties.getClueSubmitIdempotencyTtl());
        entries.put(new Scope(userId, key), Entry.unused(expiresAt));
        return new IdempotencyKeyResponse(key, OffsetDateTime.ofInstant(expiresAt, TimeUtils.ZONE));
    }

    public AcquireResult acquire(Long userId, String rawKey, String fingerprint) {
        String key = normalizeKey(rawKey);
        Scope scope = new Scope(userId, key);
        Instant now = Instant.now();
        AtomicReference<AcquireResult> result = new AtomicReference<>();

        entries.compute(scope, (ignored, current) -> {
            if (current == null || (current.state != State.PROCESSING && !current.expiresAt.isAfter(now))) {
                result.set(AcquireResult.invalid());
                return null;
            }
            if (current.state == State.PROCESSING) {
                result.set(AcquireResult.inProgress());
                return current;
            }
            if (current.state == State.SUCCEEDED) {
                if (Objects.equals(current.fingerprint, fingerprint)) {
                    result.set(AcquireResult.replay(current.response));
                } else {
                    result.set(AcquireResult.reused());
                }
                return current;
            }
            result.set(AcquireResult.proceed());
            return current.processing(fingerprint);
        });

        AcquireResult acquired = result.get();
        if (acquired == null || acquired.kind == AcquireKind.INVALID) {
            throw new BusinessException(ErrorCode.INVALID_IDEMPOTENCY_KEY);
        }
        if (acquired.kind == AcquireKind.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_REQUEST_IN_PROGRESS);
        }
        if (acquired.kind == AcquireKind.REUSED) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
        }
        return acquired;
    }

    public void markSucceeded(Long userId, String rawKey, String fingerprint, CreateClueResponse response) {
        String key = normalizeKey(rawKey);
        Scope scope = new Scope(userId, key);
        entries.computeIfPresent(scope, (ignored, current) -> {
            if (current.state == State.PROCESSING && Objects.equals(current.fingerprint, fingerprint)) {
                return current.succeeded(response);
            }
            log.error("Idempotency state mismatch on afterCommit for userId={}, key={}", userId, key);
            return current;
        });
    }

    public void releaseToUnused(Long userId, String rawKey, String fingerprint) {
        String key;
        try {
            key = normalizeKey(rawKey);
        } catch (BusinessException ex) {
            return;
        }
        Scope scope = new Scope(userId, key);
        Instant now = Instant.now();
        entries.computeIfPresent(scope, (ignored, current) -> {
            if (current.state == State.PROCESSING && Objects.equals(current.fingerprint, fingerprint)) {
                if (!current.expiresAt.isAfter(now)) {
                    return null;
                }
                return Entry.unused(current.expiresAt);
            }
            return current;
        });
    }

    @Scheduled(fixedDelayString = "${petlink.api.clue-idempotency-cleanup-interval-ms:60000}")
    public void cleanupExpired() {
        Instant now = Instant.now();
        entries.entrySet().removeIf(e -> e.getValue().state != State.PROCESSING && !e.getValue().expiresAt.isAfter(now));
    }

    private String normalizeKey(String rawKey) {
        if (rawKey == null) {
            throw new BusinessException(ErrorCode.INVALID_IDEMPOTENCY_KEY);
        }
        String value = rawKey.trim();
        try {
            UUID uuid = UUID.fromString(value);
            String canonical = uuid.toString();
            if (!canonical.equals(value.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("non-canonical UUID");
            }
            return canonical;
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.INVALID_IDEMPOTENCY_KEY);
        }
    }

    public static class AcquireResult {
        private final AcquireKind kind;
        private final CreateClueResponse replayResponse;

        private AcquireResult(AcquireKind kind, CreateClueResponse replayResponse) {
            this.kind = kind;
            this.replayResponse = replayResponse;
        }

        static AcquireResult proceed() { return new AcquireResult(AcquireKind.PROCEED, null); }
        static AcquireResult replay(CreateClueResponse response) { return new AcquireResult(AcquireKind.REPLAY, response); }
        static AcquireResult invalid() { return new AcquireResult(AcquireKind.INVALID, null); }
        static AcquireResult inProgress() { return new AcquireResult(AcquireKind.IN_PROGRESS, null); }
        static AcquireResult reused() { return new AcquireResult(AcquireKind.REUSED, null); }

        public boolean isReplay() { return kind == AcquireKind.REPLAY; }
        public CreateClueResponse getReplayResponse() { return replayResponse; }
    }

    private enum AcquireKind { PROCEED, REPLAY, INVALID, IN_PROGRESS, REUSED }

    private static class Scope {
        private final Long userId;
        private final String key;

        private Scope(Long userId, String key) {
            this.userId = userId;
            this.key = key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Scope)) return false;
            Scope scope = (Scope) o;
            return Objects.equals(userId, scope.userId) && Objects.equals(key, scope.key);
        }

        @Override
        public int hashCode() { return Objects.hash(userId, key); }
    }

    private static class Entry {
        private final State state;
        private final Instant expiresAt;
        private final String fingerprint;
        private final CreateClueResponse response;

        private Entry(State state, Instant expiresAt, String fingerprint, CreateClueResponse response) {
            this.state = state;
            this.expiresAt = expiresAt;
            this.fingerprint = fingerprint;
            this.response = response;
        }

        static Entry unused(Instant expiresAt) { return new Entry(State.UNUSED, expiresAt, null, null); }
        Entry processing(String fingerprint) { return new Entry(State.PROCESSING, expiresAt, fingerprint, null); }
        Entry succeeded(CreateClueResponse response) { return new Entry(State.SUCCEEDED, expiresAt, fingerprint, response); }
    }
}

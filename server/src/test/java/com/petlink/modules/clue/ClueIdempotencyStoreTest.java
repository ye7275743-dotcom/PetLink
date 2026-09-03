package com.petlink.modules.clue;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.modules.clue.config.ClueApiProperties;
import com.petlink.modules.clue.service.ClueIdempotencyStore;
import com.petlink.modules.clue.vo.CreateClueResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class ClueIdempotencyStoreTest {
    private ClueIdempotencyStore store;

    @BeforeEach
    void setUp() {
        store = new ClueIdempotencyStore(new ClueApiProperties());
    }

    @Test
    void sameSucceededFingerprintReplaysFirstResponse() {
        String key = store.issue(1001L).getIdempotencyKey();
        var first = store.acquire(1001L, key, "fp-a");
        assertFalse(first.isReplay());
        CreateClueResponse response = new CreateClueResponse("3001", "PENDING_REVIEW",
                OffsetDateTime.parse("2026-08-29T09:45:00+08:00"));
        store.markSucceeded(1001L, key, "fp-a", response);

        var replay = store.acquire(1001L, key, "fp-a");
        assertTrue(replay.isReplay());
        assertSame(response, replay.getReplayResponse());
    }

    @Test
    void processingKeyReturns40905AndRollbackMakesItReusable() {
        String key = store.issue(1001L).getIdempotencyKey();
        store.acquire(1001L, key, "fp-a");
        BusinessException inProgress = assertThrows(BusinessException.class,
                () -> store.acquire(1001L, key, "fp-a"));
        assertEquals(ErrorCode.IDEMPOTENCY_REQUEST_IN_PROGRESS, inProgress.getErrorCode());

        store.releaseToUnused(1001L, key, "fp-a");
        assertFalse(store.acquire(1001L, key, "fp-b").isReplay());
    }

    @Test
    void succeededKeyWithDifferentFingerprintReturns40906() {
        String key = store.issue(1001L).getIdempotencyKey();
        store.acquire(1001L, key, "fp-a");
        store.markSucceeded(1001L, key, "fp-a",
                new CreateClueResponse("3001", "PENDING_REVIEW", OffsetDateTime.now()));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> store.acquire(1001L, key, "fp-b"));
        assertEquals(ErrorCode.IDEMPOTENCY_KEY_REUSED, ex.getErrorCode());
    }

    @Test
    void concurrentAcquireOnSameKeyAllowsOnlyOneToProceed() throws Exception {
        String key = store.issue(1001L).getIdempotencyKey();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Object> first = pool.submit(() -> acquireAfter(start, key));
            Future<Object> second = pool.submit(() -> acquireAfter(start, key));
            start.countDown();

            Object r1 = first.get();
            Object r2 = second.get();
            long proceeds = List.of(r1, r2).stream()
                    .filter(ClueIdempotencyStore.AcquireResult.class::isInstance).count();
            long inProgress = List.of(r1, r2).stream()
                    .filter(BusinessException.class::isInstance)
                    .map(BusinessException.class::cast)
                    .filter(ex -> ex.getErrorCode() == ErrorCode.IDEMPOTENCY_REQUEST_IN_PROGRESS)
                    .count();
            assertEquals(1, proceeds);
            assertEquals(1, inProgress);
        } finally {
            pool.shutdownNow();
        }
    }

    private Object acquireAfter(CountDownLatch start, String key) {
        try {
            start.await();
            return store.acquire(1001L, key, "same-fingerprint");
        } catch (BusinessException ex) {
            return ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }
    }

    @Test
    void keyIsScopedToIssuingUser() {
        String key = store.issue(1001L).getIdempotencyKey();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> store.acquire(2002L, key, "fp-a"));
        assertEquals(ErrorCode.INVALID_IDEMPOTENCY_KEY, ex.getErrorCode());
    }
}

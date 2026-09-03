package com.petlink.modules.clue.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "petlink.api")
public class ClueApiProperties {
    @NotNull
    private Duration clueSubmitIdempotencyTtl = Duration.ofMinutes(10);
    private long clueIdempotencyCleanupIntervalMs = 60000L;

    @AssertTrue(message = "petlink.api.clue-submit-idempotency-ttl must be positive")
    public boolean isClueSubmitIdempotencyTtlValid() {
        return clueSubmitIdempotencyTtl != null
                && !clueSubmitIdempotencyTtl.isZero()
                && !clueSubmitIdempotencyTtl.isNegative();
    }

    @AssertTrue(message = "petlink.api.clue-idempotency-cleanup-interval-ms must be positive")
    public boolean isClueIdempotencyCleanupIntervalValid() {
        return clueIdempotencyCleanupIntervalMs > 0;
    }

    public Duration getClueSubmitIdempotencyTtl() { return clueSubmitIdempotencyTtl; }
    public void setClueSubmitIdempotencyTtl(Duration value) { this.clueSubmitIdempotencyTtl = value; }
    public long getClueIdempotencyCleanupIntervalMs() { return clueIdempotencyCleanupIntervalMs; }
    public void setClueIdempotencyCleanupIntervalMs(long value) { this.clueIdempotencyCleanupIntervalMs = value; }
}

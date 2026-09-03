package com.petlink.modules.clue.vo;

import java.time.OffsetDateTime;

public class IdempotencyKeyResponse {
    private String idempotencyKey;
    private OffsetDateTime expiresAt;

    public IdempotencyKeyResponse(String idempotencyKey, OffsetDateTime expiresAt) {
        this.idempotencyKey = idempotencyKey;
        this.expiresAt = expiresAt;
    }

    public String getIdempotencyKey() { return idempotencyKey; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
}

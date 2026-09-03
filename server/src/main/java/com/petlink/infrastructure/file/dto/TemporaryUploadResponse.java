package com.petlink.infrastructure.file.dto;

import java.time.OffsetDateTime;

public class TemporaryUploadResponse {
    private final String token;
    private final OffsetDateTime expiresAt;

    public TemporaryUploadResponse(String token, OffsetDateTime expiresAt) {
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public String getToken() { return token; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
}

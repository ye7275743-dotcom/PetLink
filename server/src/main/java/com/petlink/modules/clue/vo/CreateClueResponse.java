package com.petlink.modules.clue.vo;

import java.time.OffsetDateTime;

public class CreateClueResponse {
    private String clueId;
    private String status;
    private OffsetDateTime createdAt;

    public CreateClueResponse(String clueId, String status, OffsetDateTime createdAt) {
        this.clueId = clueId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getClueId() { return clueId; }
    public String getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}

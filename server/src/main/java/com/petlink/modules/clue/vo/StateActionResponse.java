package com.petlink.modules.clue.vo;

import java.time.OffsetDateTime;

public class StateActionResponse {
    private String id;
    private String status;
    private OffsetDateTime updatedAt;

    public StateActionResponse(String id, String status, OffsetDateTime updatedAt) {
        this.id = id;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getStatus() { return status; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}

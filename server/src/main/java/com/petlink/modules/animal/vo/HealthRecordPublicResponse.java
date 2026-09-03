package com.petlink.modules.animal.vo;

import java.time.OffsetDateTime;

public class HealthRecordPublicResponse {
    private final String id;
    private final String content;
    private final OffsetDateTime createdAt;
    public HealthRecordPublicResponse(String id, String content, OffsetDateTime createdAt) {
        this.id=id; this.content=content; this.createdAt=createdAt;
    }
    public String getId() { return id; }
    public String getContent() { return content; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}

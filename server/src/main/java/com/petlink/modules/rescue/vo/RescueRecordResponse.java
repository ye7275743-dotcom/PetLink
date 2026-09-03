package com.petlink.modules.rescue.vo;

import java.time.OffsetDateTime;

public class RescueRecordResponse {
    private final String id;
    private final String taskId;
    private final String content;
    private final OffsetDateTime createdAt;
    public RescueRecordResponse(String id, String taskId, String content, OffsetDateTime createdAt) {
        this.id = id; this.taskId = taskId; this.content = content; this.createdAt = createdAt;
    }
    public String getId() { return id; }
    public String getTaskId() { return taskId; }
    public String getContent() { return content; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}

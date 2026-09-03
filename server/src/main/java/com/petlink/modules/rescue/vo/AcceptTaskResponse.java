package com.petlink.modules.rescue.vo;

import java.time.OffsetDateTime;

public class AcceptTaskResponse {
    private final String taskId;
    private final String clueId;
    private final String status;
    private final OffsetDateTime createdAt;

    public AcceptTaskResponse(String taskId, String clueId, String status, OffsetDateTime createdAt) {
        this.taskId = taskId;
        this.clueId = clueId;
        this.status = status;
        this.createdAt = createdAt;
    }
    public String getTaskId() { return taskId; }
    public String getClueId() { return clueId; }
    public String getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}

package com.petlink.modules.rescue.vo;

import java.time.OffsetDateTime;

public class TaskSummaryResponse {
    private final String id;
    private final String clueId;
    private final String rescuerId;
    private final String status;
    private final OffsetDateTime startedAt;
    private final OffsetDateTime finishedAt;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public TaskSummaryResponse(String id, String clueId, String rescuerId, String status, OffsetDateTime startedAt,
                               OffsetDateTime finishedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.clueId = clueId;
        this.rescuerId = rescuerId;
        this.status = status;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    public String getId() { return id; }
    public String getClueId() { return clueId; }
    public String getRescuerId() { return rescuerId; }
    public String getStatus() { return status; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getFinishedAt() { return finishedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}

package com.petlink.modules.rescue.vo;

import com.petlink.modules.clue.vo.ClueSummaryResponse;
import java.time.OffsetDateTime;
import java.util.List;

public class TaskDetailResponse extends TaskSummaryResponse {
    private final ClueSummaryResponse clue;
    private final String failureReason;
    private final String cancelReason;
    private final List<RescueRecordResponse> records;
    private final List<AnimalSummaryResponse> animals;

    public TaskDetailResponse(String id, String clueId, String status, OffsetDateTime startedAt, OffsetDateTime finishedAt,
                              OffsetDateTime createdAt, OffsetDateTime updatedAt, ClueSummaryResponse clue,
                              String failureReason, String cancelReason, List<RescueRecordResponse> records,
                              List<AnimalSummaryResponse> animals) {
        super(id, clueId, status, startedAt, finishedAt, createdAt, updatedAt);
        this.clue = clue;
        this.failureReason = failureReason;
        this.cancelReason = cancelReason;
        this.records = records;
        this.animals = animals;
    }
    public ClueSummaryResponse getClue(){ return clue; }
    public String getFailureReason(){ return failureReason; }
    public String getCancelReason(){ return cancelReason; }
    public List<RescueRecordResponse> getRecords(){ return records; }
    public List<AnimalSummaryResponse> getAnimals(){ return animals; }
}

package com.petlink.modules.rescue.vo;

import java.time.OffsetDateTime;
import java.util.List;

public class RescueSuccessResponse {
    private final String taskId;
    private final String status;
    private final List<String> animalIds;
    private final OffsetDateTime finishedAt;
    public RescueSuccessResponse(String taskId, String status, List<String> animalIds, OffsetDateTime finishedAt) {
        this.taskId=taskId; this.status=status; this.animalIds=animalIds; this.finishedAt=finishedAt;
    }
    public String getTaskId(){ return taskId; }
    public String getStatus(){ return status; }
    public List<String> getAnimalIds(){ return animalIds; }
    public OffsetDateTime getFinishedAt(){ return finishedAt; }
}

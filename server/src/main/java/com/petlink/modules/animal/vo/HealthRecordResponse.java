package com.petlink.modules.animal.vo;

import java.time.OffsetDateTime;

public class HealthRecordResponse extends HealthRecordPublicResponse {
    private final String recorderId;
    public HealthRecordResponse(String id, String recorderId, String content, OffsetDateTime createdAt) {
        super(id, content, createdAt); this.recorderId=recorderId;
    }
    public String getRecorderId() { return recorderId; }
}

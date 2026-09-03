package com.petlink.modules.animal.vo;

import java.time.OffsetDateTime;
import java.util.List;

public class AnimalDetailResponse extends AnimalSummaryResponse {
    private final String rescueTaskId;
    private final String suspendReason;
    private final List<AnimalImageResponse> images;
    private final List<HealthRecordPublicResponse> healthRecords;

    public AnimalDetailResponse(String id, String name, String species, String sex, Integer estimatedAgeMonths,
                                String color, String healthCondition, String status, String coverImageUrl,
                                Integer version, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                                String rescueTaskId, String suspendReason, List<AnimalImageResponse> images,
                                List<HealthRecordPublicResponse> healthRecords) {
        super(id,name,species,sex,estimatedAgeMonths,color,healthCondition,status,coverImageUrl,version,createdAt,updatedAt);
        this.rescueTaskId=rescueTaskId; this.suspendReason=suspendReason; this.images=images; this.healthRecords=healthRecords;
    }
    public String getRescueTaskId(){return rescueTaskId;}
    public String getSuspendReason(){return suspendReason;}
    public List<AnimalImageResponse> getImages(){return images;}
    public List<HealthRecordPublicResponse> getHealthRecords(){return healthRecords;}
}

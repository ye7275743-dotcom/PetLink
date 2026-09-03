package com.petlink.modules.followup.vo;

import java.time.OffsetDateTime;
import java.util.List;

public class FollowUpRecordResponse {
    private final String id;
    private final String adoptionRecordId;
    private final String submitterId;
    private final String content;
    private final String healthCondition;
    private final List<FollowUpImageResponse> images;
    private final OffsetDateTime createdAt;

    public FollowUpRecordResponse(String id, String adoptionRecordId, String submitterId,
                                  String content, String healthCondition,
                                  List<FollowUpImageResponse> images, OffsetDateTime createdAt) {
        this.id=id; this.adoptionRecordId=adoptionRecordId; this.submitterId=submitterId;
        this.content=content; this.healthCondition=healthCondition; this.images=List.copyOf(images); this.createdAt=createdAt;
    }
    public String getId(){ return id; }
    public String getAdoptionRecordId(){ return adoptionRecordId; }
    public String getSubmitterId(){ return submitterId; }
    public String getContent(){ return content; }
    public String getHealthCondition(){ return healthCondition; }
    public List<FollowUpImageResponse> getImages(){ return images; }
    public OffsetDateTime getCreatedAt(){ return createdAt; }
}

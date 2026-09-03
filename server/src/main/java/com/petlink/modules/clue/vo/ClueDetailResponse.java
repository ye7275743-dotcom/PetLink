package com.petlink.modules.clue.vo;

import java.time.OffsetDateTime;
import java.util.List;

public class ClueDetailResponse extends ClueSummaryResponse {
    private String sceneDescription;
    private String contact;
    private String rejectReason;
    private String reviewerId;
    private OffsetDateTime reviewedAt;
    private List<ClueImageResponse> images;

    public ClueDetailResponse(String id, String location, OffsetDateTime foundTime,
                              String animalDescription, String status, String coverImageUrl,
                              OffsetDateTime createdAt, OffsetDateTime updatedAt,
                              String sceneDescription, String contact, String rejectReason,
                              String reviewerId, OffsetDateTime reviewedAt,
                              List<ClueImageResponse> images) {
        super(id, location, foundTime, animalDescription, status, coverImageUrl, createdAt, updatedAt);
        this.sceneDescription = sceneDescription;
        this.contact = contact;
        this.rejectReason = rejectReason;
        this.reviewerId = reviewerId;
        this.reviewedAt = reviewedAt;
        this.images = images;
    }

    public String getSceneDescription() { return sceneDescription; }
    public String getContact() { return contact; }
    public String getRejectReason() { return rejectReason; }
    public String getReviewerId() { return reviewerId; }
    public OffsetDateTime getReviewedAt() { return reviewedAt; }
    public List<ClueImageResponse> getImages() { return images; }
}

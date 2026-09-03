package com.petlink.modules.clue.vo;

import java.time.OffsetDateTime;

public class ClueSummaryResponse {
    private String id;
    private String location;
    private OffsetDateTime foundTime;
    private String animalDescription;
    private String status;
    private String coverImageUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public ClueSummaryResponse(String id, String location, OffsetDateTime foundTime,
                               String animalDescription, String status, String coverImageUrl,
                               OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.location = location;
        this.foundTime = foundTime;
        this.animalDescription = animalDescription;
        this.status = status;
        this.coverImageUrl = coverImageUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getLocation() { return location; }
    public OffsetDateTime getFoundTime() { return foundTime; }
    public String getAnimalDescription() { return animalDescription; }
    public String getStatus() { return status; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}

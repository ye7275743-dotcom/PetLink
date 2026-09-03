package com.petlink.modules.content.vo;

import java.time.OffsetDateTime;

public class AnnouncementAdminSummaryResponse {
    private final String id;
    private final String title;
    private final String status;
    private final OffsetDateTime publishedAt;
    private final Integer version;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    public AnnouncementAdminSummaryResponse(String id,String title,String status,OffsetDateTime publishedAt,Integer version,OffsetDateTime createdAt,OffsetDateTime updatedAt){this.id=id;this.title=title;this.status=status;this.publishedAt=publishedAt;this.version=version;this.createdAt=createdAt;this.updatedAt=updatedAt;}
    public String getId(){return id;} public String getTitle(){return title;} public String getStatus(){return status;}
    public OffsetDateTime getPublishedAt(){return publishedAt;} public Integer getVersion(){return version;}
    public OffsetDateTime getCreatedAt(){return createdAt;} public OffsetDateTime getUpdatedAt(){return updatedAt;}
}

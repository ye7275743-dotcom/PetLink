package com.petlink.modules.content.vo;

import java.time.OffsetDateTime;

public class AnnouncementAdminDetailResponse {
    private final String id;
    private final String title;
    private final String content;
    private final String status;
    private final String createdBy;
    private final String updatedBy;
    private final OffsetDateTime publishedAt;
    private final Integer version;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    public AnnouncementAdminDetailResponse(String id,String title,String content,String status,String createdBy,String updatedBy,OffsetDateTime publishedAt,Integer version,OffsetDateTime createdAt,OffsetDateTime updatedAt){this.id=id;this.title=title;this.content=content;this.status=status;this.createdBy=createdBy;this.updatedBy=updatedBy;this.publishedAt=publishedAt;this.version=version;this.createdAt=createdAt;this.updatedAt=updatedAt;}
    public String getId(){return id;} public String getTitle(){return title;} public String getContent(){return content;} public String getStatus(){return status;}
    public String getCreatedBy(){return createdBy;} public String getUpdatedBy(){return updatedBy;} public OffsetDateTime getPublishedAt(){return publishedAt;}
    public Integer getVersion(){return version;} public OffsetDateTime getCreatedAt(){return createdAt;} public OffsetDateTime getUpdatedAt(){return updatedAt;}
}

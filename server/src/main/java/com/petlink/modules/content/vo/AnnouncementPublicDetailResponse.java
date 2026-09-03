package com.petlink.modules.content.vo;

import java.time.OffsetDateTime;

public class AnnouncementPublicDetailResponse {
    private final String id;
    private final String title;
    private final String content;
    private final OffsetDateTime publishedAt;
    public AnnouncementPublicDetailResponse(String id, String title, String content, OffsetDateTime publishedAt) { this.id=id; this.title=title; this.content=content; this.publishedAt=publishedAt; }
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public OffsetDateTime getPublishedAt() { return publishedAt; }
}

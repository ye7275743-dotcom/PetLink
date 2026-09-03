package com.petlink.modules.content.vo;

import java.time.OffsetDateTime;

public class AnnouncementPublicSummaryResponse {
    private final String id;
    private final String title;
    private final OffsetDateTime publishedAt;
    public AnnouncementPublicSummaryResponse(String id, String title, OffsetDateTime publishedAt) { this.id=id; this.title=title; this.publishedAt=publishedAt; }
    public String getId() { return id; }
    public String getTitle() { return title; }
    public OffsetDateTime getPublishedAt() { return publishedAt; }
}

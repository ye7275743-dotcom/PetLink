package com.petlink.modules.content.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class PatchAnnouncementRequest {
    private String title;
    private String content;
    private Integer version;
    @JsonIgnore private boolean titlePresent;
    @JsonIgnore private boolean contentPresent;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.titlePresent = true; this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.contentPresent = true; this.content = content; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public boolean isTitlePresent() { return titlePresent; }
    public boolean isContentPresent() { return contentPresent; }
}

package com.petlink.modules.followup.vo;

public class FollowUpImageResponse {
    private final String id;
    private final String url;
    private final Integer sortOrder;

    public FollowUpImageResponse(String id, String url, Integer sortOrder) {
        this.id=id; this.url=url; this.sortOrder=sortOrder;
    }
    public String getId(){ return id; }
    public String getUrl(){ return url; }
    public Integer getSortOrder(){ return sortOrder; }
}

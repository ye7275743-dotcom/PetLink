package com.petlink.modules.animal.vo;

public class AnimalImageResponse {
    private final String id;
    private final String url;
    private final Integer sortOrder;
    public AnimalImageResponse(String id, String url, Integer sortOrder) { this.id=id; this.url=url; this.sortOrder=sortOrder; }
    public String getId() { return id; }
    public String getUrl() { return url; }
    public Integer getSortOrder() { return sortOrder; }
}

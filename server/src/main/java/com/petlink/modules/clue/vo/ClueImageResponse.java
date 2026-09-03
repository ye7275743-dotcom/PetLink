package com.petlink.modules.clue.vo;

public class ClueImageResponse {
    private String id;
    private String url;
    private int sortOrder;

    public ClueImageResponse(String id, String url, int sortOrder) {
        this.id = id;
        this.url = url;
        this.sortOrder = sortOrder;
    }

    public String getId() { return id; }
    public String getUrl() { return url; }
    public int getSortOrder() { return sortOrder; }
}

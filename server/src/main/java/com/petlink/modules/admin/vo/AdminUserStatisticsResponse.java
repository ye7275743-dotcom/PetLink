package com.petlink.modules.admin.vo;

public class AdminUserStatisticsResponse {
    private final long publishedClueCount;
    private final long rescueTaskCount;
    private final long adoptionApplicationCount;
    private final long adoptionRecordCount;

    public AdminUserStatisticsResponse(long publishedClueCount,long rescueTaskCount,long adoptionApplicationCount,long adoptionRecordCount) {
        this.publishedClueCount=publishedClueCount;this.rescueTaskCount=rescueTaskCount;
        this.adoptionApplicationCount=adoptionApplicationCount;this.adoptionRecordCount=adoptionRecordCount;
    }
    public long getPublishedClueCount(){return publishedClueCount;} public long getRescueTaskCount(){return rescueTaskCount;}
    public long getAdoptionApplicationCount(){return adoptionApplicationCount;} public long getAdoptionRecordCount(){return adoptionRecordCount;}
}

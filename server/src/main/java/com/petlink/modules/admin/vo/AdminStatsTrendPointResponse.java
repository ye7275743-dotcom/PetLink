package com.petlink.modules.admin.vo;

public class AdminStatsTrendPointResponse {
    private final String date;
    private final long rescueSuccessCount;
    private final long adoptionCount;

    public AdminStatsTrendPointResponse(String date,long rescueSuccessCount,long adoptionCount) {
        this.date=date;this.rescueSuccessCount=rescueSuccessCount;this.adoptionCount=adoptionCount;
    }
    public String getDate(){return date;} public long getRescueSuccessCount(){return rescueSuccessCount;}
    public long getAdoptionCount(){return adoptionCount;}
}

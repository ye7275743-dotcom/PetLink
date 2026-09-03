package com.petlink.modules.admin.vo;

import java.util.List;

public class AdminStatsTrendsResponse {
    private final String from;
    private final String to;
    private final String granularity;
    private final String zoneId;
    private final List<AdminStatsTrendPointResponse> points;

    public AdminStatsTrendsResponse(String from,String to,String granularity,String zoneId,List<AdminStatsTrendPointResponse> points) {
        this.from=from;this.to=to;this.granularity=granularity;this.zoneId=zoneId;this.points=points;
    }
    public String getFrom(){return from;} public String getTo(){return to;} public String getGranularity(){return granularity;}
    public String getZoneId(){return zoneId;} public List<AdminStatsTrendPointResponse> getPoints(){return points;}
}

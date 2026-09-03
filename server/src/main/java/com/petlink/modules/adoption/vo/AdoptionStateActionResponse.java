package com.petlink.modules.adoption.vo;

import java.time.OffsetDateTime;

public class AdoptionStateActionResponse {
    private final String id;
    private final String status;
    private final OffsetDateTime updatedAt;
    public AdoptionStateActionResponse(String id,String status,OffsetDateTime updatedAt){this.id=id;this.status=status;this.updatedAt=updatedAt;}
    public String getId(){return id;} public String getStatus(){return status;} public OffsetDateTime getUpdatedAt(){return updatedAt;}
}


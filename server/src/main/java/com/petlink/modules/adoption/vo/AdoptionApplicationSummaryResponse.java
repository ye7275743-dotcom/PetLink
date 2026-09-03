package com.petlink.modules.adoption.vo;

import java.time.OffsetDateTime;

public class AdoptionApplicationSummaryResponse {
    private final String id;
    private final String animalId;
    private final String animalName;
    private final String status;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    public AdoptionApplicationSummaryResponse(String id,String animalId,String animalName,String status,OffsetDateTime createdAt,OffsetDateTime updatedAt){
        this.id=id;this.animalId=animalId;this.animalName=animalName;this.status=status;this.createdAt=createdAt;this.updatedAt=updatedAt;
    }
    public String getId(){return id;} public String getAnimalId(){return animalId;} public String getAnimalName(){return animalName;} public String getStatus(){return status;} public OffsetDateTime getCreatedAt(){return createdAt;} public OffsetDateTime getUpdatedAt(){return updatedAt;}
}


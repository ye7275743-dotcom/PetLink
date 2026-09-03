package com.petlink.modules.adoption.vo;

import java.time.OffsetDateTime;

public class AdoptionOverviewResponse {
    private final String animalId;
    private final String animalStatus;
    private final long pendingApplicationCount;
    private final String approvedApplicationId;
    private final String adoptionRecordId;
    private final OffsetDateTime adoptedAt;
    public AdoptionOverviewResponse(String animalId,String animalStatus,long pendingApplicationCount,String approvedApplicationId,String adoptionRecordId,OffsetDateTime adoptedAt){
        this.animalId=animalId;this.animalStatus=animalStatus;this.pendingApplicationCount=pendingApplicationCount;this.approvedApplicationId=approvedApplicationId;this.adoptionRecordId=adoptionRecordId;this.adoptedAt=adoptedAt;
    }
    public String getAnimalId(){return animalId;} public String getAnimalStatus(){return animalStatus;} public long getPendingApplicationCount(){return pendingApplicationCount;}
    public String getApprovedApplicationId(){return approvedApplicationId;} public String getAdoptionRecordId(){return adoptionRecordId;} public OffsetDateTime getAdoptedAt(){return adoptedAt;}
}


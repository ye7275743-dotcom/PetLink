package com.petlink.modules.adoption.vo;

import java.time.OffsetDateTime;

public class AdoptionRecordResponse {
    private final String id;
    private final String applicationId;
    private final String animalId;
    private final String userId;
    private final OffsetDateTime adoptedAt;
    private final OffsetDateTime createdAt;
    private final AdoptionAnimalResponse animal;
    public AdoptionRecordResponse(String id,String applicationId,String animalId,String userId,OffsetDateTime adoptedAt,OffsetDateTime createdAt,AdoptionAnimalResponse animal){
        this.id=id;this.applicationId=applicationId;this.animalId=animalId;this.userId=userId;this.adoptedAt=adoptedAt;this.createdAt=createdAt;this.animal=animal;
    }
    public String getId(){return id;} public String getApplicationId(){return applicationId;} public String getAnimalId(){return animalId;} public String getUserId(){return userId;}
    public OffsetDateTime getAdoptedAt(){return adoptedAt;} public OffsetDateTime getCreatedAt(){return createdAt;} public AdoptionAnimalResponse getAnimal(){return animal;}
}


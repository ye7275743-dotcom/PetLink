package com.petlink.modules.adoption.vo;

import java.time.OffsetDateTime;

public class AdoptionApplicationDetailResponse extends AdoptionApplicationSummaryResponse {
    private final String userId;
    private final String adoptionReason;
    private final String housingCondition;
    private final String familyMembers;
    private final String petExperience;
    private final String contact;
    private final String reviewerId;
    private final OffsetDateTime reviewedAt;
    private final String rejectReason;
    private final AdoptionAnimalResponse animal;
    public AdoptionApplicationDetailResponse(String id,String animalId,String animalName,String status,OffsetDateTime createdAt,OffsetDateTime updatedAt,
            String userId,String adoptionReason,String housingCondition,String familyMembers,String petExperience,String contact,
            String reviewerId,OffsetDateTime reviewedAt,String rejectReason,AdoptionAnimalResponse animal){
        super(id,animalId,userId,animalName,status,createdAt,updatedAt); this.userId=userId; this.adoptionReason=adoptionReason; this.housingCondition=housingCondition;
        this.familyMembers=familyMembers; this.petExperience=petExperience; this.contact=contact; this.reviewerId=reviewerId; this.reviewedAt=reviewedAt; this.rejectReason=rejectReason; this.animal=animal;
    }
    public String getUserId(){return userId;} public String getAdoptionReason(){return adoptionReason;} public String getHousingCondition(){return housingCondition;}
    public String getFamilyMembers(){return familyMembers;} public String getPetExperience(){return petExperience;} public String getContact(){return contact;}
    public String getReviewerId(){return reviewerId;} public OffsetDateTime getReviewedAt(){return reviewedAt;} public String getRejectReason(){return rejectReason;} public AdoptionAnimalResponse getAnimal(){return animal;}
}

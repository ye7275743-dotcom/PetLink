package com.petlink.modules.adoption.dto;

public class SubmitAdoptionApplicationRequest {
    private String adoptionReason;
    private String housingCondition;
    private String familyMembers;
    private String petExperience;
    private String contact;
    public String getAdoptionReason(){return adoptionReason;} public void setAdoptionReason(String v){this.adoptionReason=v;}
    public String getHousingCondition(){return housingCondition;} public void setHousingCondition(String v){this.housingCondition=v;}
    public String getFamilyMembers(){return familyMembers;} public void setFamilyMembers(String v){this.familyMembers=v;}
    public String getPetExperience(){return petExperience;} public void setPetExperience(String v){this.petExperience=v;}
    public String getContact(){return contact;} public void setContact(String v){this.contact=v;}
}


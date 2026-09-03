package com.petlink.modules.adoption.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("adoption_application")
public class AdoptionApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long animalId;
    private String adoptionReason;
    private String housingCondition;
    private String familyMembers;
    private String petExperience;
    private String contact;
    private String status;
    private Long reviewerId;
    private LocalDateTime reviewedAt;
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getUserId(){return userId;} public void setUserId(Long userId){this.userId=userId;}
    public Long getAnimalId(){return animalId;} public void setAnimalId(Long animalId){this.animalId=animalId;}
    public String getAdoptionReason(){return adoptionReason;} public void setAdoptionReason(String v){this.adoptionReason=v;}
    public String getHousingCondition(){return housingCondition;} public void setHousingCondition(String v){this.housingCondition=v;}
    public String getFamilyMembers(){return familyMembers;} public void setFamilyMembers(String v){this.familyMembers=v;}
    public String getPetExperience(){return petExperience;} public void setPetExperience(String v){this.petExperience=v;}
    public String getContact(){return contact;} public void setContact(String v){this.contact=v;}
    public String getStatus(){return status;} public void setStatus(String v){this.status=v;}
    public Long getReviewerId(){return reviewerId;} public void setReviewerId(Long v){this.reviewerId=v;}
    public LocalDateTime getReviewedAt(){return reviewedAt;} public void setReviewedAt(LocalDateTime v){this.reviewedAt=v;}
    public String getRejectReason(){return rejectReason;} public void setRejectReason(String v){this.rejectReason=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){this.createdAt=v;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){this.updatedAt=v;}
}


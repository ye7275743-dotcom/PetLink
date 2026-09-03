package com.petlink.modules.clue.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("rescue_clue")
public class RescueClue {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long publisherId;
    private String location;
    private LocalDateTime foundTime;
    private String animalDescription;
    private String sceneDescription;
    private String contact;
    private String status;
    private Long reviewerId;
    private LocalDateTime reviewedAt;
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPublisherId() { return publisherId; }
    public void setPublisherId(Long publisherId) { this.publisherId = publisherId; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public LocalDateTime getFoundTime() { return foundTime; }
    public void setFoundTime(LocalDateTime foundTime) { this.foundTime = foundTime; }
    public String getAnimalDescription() { return animalDescription; }
    public void setAnimalDescription(String animalDescription) { this.animalDescription = animalDescription; }
    public String getSceneDescription() { return sceneDescription; }
    public void setSceneDescription(String sceneDescription) { this.sceneDescription = sceneDescription; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

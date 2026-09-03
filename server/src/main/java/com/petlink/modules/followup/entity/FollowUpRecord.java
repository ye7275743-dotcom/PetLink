package com.petlink.modules.followup.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("follow_up_record")
public class FollowUpRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long adoptionRecordId;
    private Long submitterId;
    private String content;
    private String healthCondition;
    private String idempotencyKey;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAdoptionRecordId() { return adoptionRecordId; }
    public void setAdoptionRecordId(Long adoptionRecordId) { this.adoptionRecordId = adoptionRecordId; }
    public Long getSubmitterId() { return submitterId; }
    public void setSubmitterId(Long submitterId) { this.submitterId = submitterId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getHealthCondition() { return healthCondition; }
    public void setHealthCondition(String healthCondition) { this.healthCondition = healthCondition; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

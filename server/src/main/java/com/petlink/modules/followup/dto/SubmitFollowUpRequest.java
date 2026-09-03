package com.petlink.modules.followup.dto;

import java.util.List;

public class SubmitFollowUpRequest {
    private String idempotencyKey;
    private String content;
    private String healthCondition;
    private List<String> imageTokens;

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getHealthCondition() { return healthCondition; }
    public void setHealthCondition(String healthCondition) { this.healthCondition = healthCondition; }
    public List<String> getImageTokens() { return imageTokens; }
    public void setImageTokens(List<String> imageTokens) { this.imageTokens = imageTokens; }
}

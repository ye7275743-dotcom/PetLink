package com.petlink.modules.rescue.dto;

public class FailureResolutionRequest {
    private String action;
    private String resolutionReason;
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getResolutionReason() { return resolutionReason; }
    public void setResolutionReason(String resolutionReason) { this.resolutionReason = resolutionReason; }
}

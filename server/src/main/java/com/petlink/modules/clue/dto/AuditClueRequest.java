package com.petlink.modules.clue.dto;

public class AuditClueRequest {
    private String decision;
    private String rejectReason;

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
}

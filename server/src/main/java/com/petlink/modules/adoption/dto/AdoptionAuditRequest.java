package com.petlink.modules.adoption.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class AdoptionAuditRequest {
    private String decision;
    private String rejectReason;
    @JsonIgnore private boolean rejectReasonPresent;
    public String getDecision(){return decision;} public void setDecision(String v){this.decision=v;}
    public String getRejectReason(){return rejectReason;} public void setRejectReason(String v){this.rejectReasonPresent=true;this.rejectReason=v;}
    public boolean isRejectReasonPresent(){return rejectReasonPresent;}
}


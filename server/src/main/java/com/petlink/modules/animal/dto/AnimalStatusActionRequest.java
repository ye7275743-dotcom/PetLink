package com.petlink.modules.animal.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class AnimalStatusActionRequest {
    private String action;
    private Integer version;
    private String suspendReason;
    @JsonIgnore private boolean suspendReasonPresent;

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getSuspendReason() { return suspendReason; }
    public void setSuspendReason(String suspendReason) { this.suspendReasonPresent = true; this.suspendReason = suspendReason; }
    public boolean isSuspendReasonPresent() { return suspendReasonPresent; }
}

package com.petlink.modules.admin.dto;

public class ChangeRoleRequest {
    private String roleCode;
    private String reason;
    public String getRoleCode(){return roleCode;}
    public void setRoleCode(String roleCode){this.roleCode=roleCode;}
    public String getReason(){return reason;}
    public void setReason(String reason){this.reason=reason;}
}

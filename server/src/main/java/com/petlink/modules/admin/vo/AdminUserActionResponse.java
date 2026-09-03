package com.petlink.modules.admin.vo;

import java.time.OffsetDateTime;

public class AdminUserActionResponse {
    private final String id;
    private final String roleCode;
    private final String status;
    private final OffsetDateTime updatedAt;

    public AdminUserActionResponse(String id,String roleCode,String status,OffsetDateTime updatedAt) {
        this.id=id;this.roleCode=roleCode;this.status=status;this.updatedAt=updatedAt;
    }
    public String getId(){return id;} public String getRoleCode(){return roleCode;} public String getStatus(){return status;}
    public OffsetDateTime getUpdatedAt(){return updatedAt;}
}

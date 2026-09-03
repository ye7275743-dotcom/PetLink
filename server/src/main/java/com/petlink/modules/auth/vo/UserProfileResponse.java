package com.petlink.modules.auth.vo;

import java.time.OffsetDateTime;

public class UserProfileResponse {
    private final String id;
    private final String account;
    private final String nickname;
    private final String phone;
    private final String roleCode;
    private final String status;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public UserProfileResponse(String id, String account, String nickname, String phone,
                               String roleCode, String status,
                               OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.account = account;
        this.nickname = nickname;
        this.phone = phone;
        this.roleCode = roleCode;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getAccount() { return account; }
    public String getNickname() { return nickname; }
    public String getPhone() { return phone; }
    public String getRoleCode() { return roleCode; }
    public String getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}

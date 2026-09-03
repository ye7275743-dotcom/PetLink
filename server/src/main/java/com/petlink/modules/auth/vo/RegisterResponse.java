package com.petlink.modules.auth.vo;

public class RegisterResponse {
    private final String userId;
    private final String account;
    private final String nickname;
    private final String roleCode;

    public RegisterResponse(String userId, String account, String nickname, String roleCode) {
        this.userId = userId;
        this.account = account;
        this.nickname = nickname;
        this.roleCode = roleCode;
    }

    public String getUserId() { return userId; }
    public String getAccount() { return account; }
    public String getNickname() { return nickname; }
    public String getRoleCode() { return roleCode; }
}

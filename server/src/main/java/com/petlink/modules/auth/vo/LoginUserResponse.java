package com.petlink.modules.auth.vo;

public class LoginUserResponse {
    private final String id;
    private final String account;
    private final String nickname;
    private final String phone;
    private final String roleCode;

    public LoginUserResponse(String id, String account, String nickname, String phone, String roleCode) {
        this.id = id;
        this.account = account;
        this.nickname = nickname;
        this.phone = phone;
        this.roleCode = roleCode;
    }

    public String getId() { return id; }
    public String getAccount() { return account; }
    public String getNickname() { return nickname; }
    public String getPhone() { return phone; }
    public String getRoleCode() { return roleCode; }
}

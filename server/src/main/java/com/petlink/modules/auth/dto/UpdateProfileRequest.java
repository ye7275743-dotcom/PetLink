package com.petlink.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class UpdateProfileRequest {
    private String nickname;
    private String phone;

    @JsonIgnore
    private boolean nicknamePresent;
    @JsonIgnore
    private boolean phonePresent;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) {
        this.nicknamePresent = true;
        this.nickname = nickname;
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) {
        this.phonePresent = true;
        this.phone = phone;
    }

    public boolean isNicknamePresent() { return nicknamePresent; }
    public boolean isPhonePresent() { return phonePresent; }
}

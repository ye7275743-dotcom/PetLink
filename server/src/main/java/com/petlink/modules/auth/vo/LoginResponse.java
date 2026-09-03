package com.petlink.modules.auth.vo;

import java.time.OffsetDateTime;

public class LoginResponse {
    private final String accessToken;
    private final String tokenType;
    private final long expiresIn;
    private final OffsetDateTime expiresAt;
    private final LoginUserResponse user;

    public LoginResponse(String accessToken, String tokenType, long expiresIn,
                         OffsetDateTime expiresAt, LoginUserResponse user) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.expiresAt = expiresAt;
        this.user = user;
    }

    public String getAccessToken() { return accessToken; }
    public String getTokenType() { return tokenType; }
    public long getExpiresIn() { return expiresIn; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public LoginUserResponse getUser() { return user; }
}

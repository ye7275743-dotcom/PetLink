package com.petlink.security;

import java.io.Serializable;

public class UserPrincipal implements Serializable {
    private final Long userId;
    private final String roleCode;

    public UserPrincipal(Long userId, String roleCode) {
        this.userId = userId;
        this.roleCode = roleCode;
    }

    public Long getUserId() { return userId; }
    public String getRoleCode() { return roleCode; }
}

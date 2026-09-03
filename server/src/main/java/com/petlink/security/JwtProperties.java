package com.petlink.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import java.nio.charset.StandardCharsets;

@Validated
@ConfigurationProperties(prefix = "petlink.jwt")
public class JwtProperties {
    @NotBlank
    private String secret;

    @Positive
    private long expiresInSeconds = 7200;

    @AssertTrue(message = "petlink.jwt.secret must contain at least 32 UTF-8 bytes")
    public boolean isSecretLengthValid() {
        return secret != null && secret.getBytes(StandardCharsets.UTF_8).length >= 32;
    }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getExpiresInSeconds() { return expiresInSeconds; }
    public void setExpiresInSeconds(long expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; }
}

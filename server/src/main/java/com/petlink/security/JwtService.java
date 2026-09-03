package com.petlink.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
public class JwtService {
    private final JwtProperties properties;
    private SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        byte[] bytes = properties.getSecret() == null
                ? new byte[0]
                : properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("PETLINK_JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public IssuedToken issue(Long userId) {
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(properties.getExpiresInSeconds());
        String token = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expires))
                .signWith(key)
                .compact();
        OffsetDateTime expiresAt = expires.atZone(ZoneId.of("Asia/Shanghai")).toOffsetDateTime();
        return new IssuedToken(token, properties.getExpiresInSeconds(), expiresAt);
    }

    public Long parseUserId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return Long.valueOf(claims.getSubject());
    }

    public static class IssuedToken {
        private final String token;
        private final long expiresIn;
        private final OffsetDateTime expiresAt;

        public IssuedToken(String token, long expiresIn, OffsetDateTime expiresAt) {
            this.token = token;
            this.expiresIn = expiresIn;
            this.expiresAt = expiresAt;
        }

        public String getToken() { return token; }
        public long getExpiresIn() { return expiresIn; }
        public OffsetDateTime getExpiresAt() { return expiresAt; }
    }
}

package com.atguigu.java.ai.langchain4j.identity;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("hbti.auth")
public record AuthProperties(
        String issuer,
        String signingKey,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        boolean secureCookies
) {
    public AuthProperties {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("hbti.auth.issuer is required");
        }
        if (signingKey == null || signingKey.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("hbti.auth.signing-key must contain at least 32 UTF-8 bytes");
        }
        if (accessTokenTtl == null || accessTokenTtl.isNegative() || accessTokenTtl.isZero()) {
            throw new IllegalArgumentException("hbti.auth.access-token-ttl must be positive");
        }
        if (refreshTokenTtl == null || refreshTokenTtl.isNegative() || refreshTokenTtl.isZero()) {
            throw new IllegalArgumentException("hbti.auth.refresh-token-ttl must be positive");
        }
    }
}

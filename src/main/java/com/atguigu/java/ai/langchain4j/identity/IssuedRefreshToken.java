package com.atguigu.java.ai.langchain4j.identity;

import java.time.Instant;

public record IssuedRefreshToken(
        String id,
        String userId,
        String value,
        Instant expiresAt
) {
}

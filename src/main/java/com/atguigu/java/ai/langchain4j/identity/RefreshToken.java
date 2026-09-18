package com.atguigu.java.ai.langchain4j.identity;

import java.time.Instant;

public record RefreshToken(
        String id,
        String userId,
        String tokenHash,
        String familyId,
        String replacedByTokenId,
        Instant expiresAt,
        Instant revokedAt,
        Instant createdAt
) {
}

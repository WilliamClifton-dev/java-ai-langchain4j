package com.atguigu.java.ai.langchain4j.identity;

import java.time.Instant;

public record UserAccount(
        String id,
        String normalizedEmail,
        String passwordHash,
        AccountStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}

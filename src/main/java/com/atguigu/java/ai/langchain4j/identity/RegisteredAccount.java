package com.atguigu.java.ai.langchain4j.identity;

import java.time.Instant;

public record RegisteredAccount(
        String id,
        String email,
        AccountStatus status,
        Instant createdAt
) {
}

package com.atguigu.java.ai.langchain4j.identity;

import java.time.Instant;

public record IssuedAccessToken(String value, Instant expiresAt) {
}

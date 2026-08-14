package com.atguigu.java.ai.langchain4j.identity.api;

import java.time.Instant;

public record AuthSessionResponse(User user, Instant accessExpiresAt) {

    public record User(String id, String email) {
    }
}

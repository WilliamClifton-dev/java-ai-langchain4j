package com.atguigu.java.ai.langchain4j.coach.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class CoachMemoryKey {
    private CoachMemoryKey() { }

    public static String forOwner(String userId, String conversationId) {
        if (userId == null || userId.isBlank() || conversationId == null
                || conversationId.isBlank()) {
            throw new IllegalArgumentException("Owner and conversation are required");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    (userId + '|' + conversationId).getBytes(StandardCharsets.UTF_8)
            );
            return "owned:" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}

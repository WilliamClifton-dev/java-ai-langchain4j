package com.atguigu.java.ai.langchain4j.coach.streaming;

import com.atguigu.java.ai.langchain4j.infrastructure.redis.EphemeralStateStore;
import com.atguigu.java.ai.langchain4j.infrastructure.redis.EphemeralStateUnavailableException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

public final class CoachRateGuard {
    private static final String KEY_PREFIX = "rate:coach:";
    private final EphemeralStateStore store;
    private final int maximumRequests;
    private final Duration window;

    public CoachRateGuard(EphemeralStateStore store, int maximumRequests, Duration window) {
        if (store == null || maximumRequests < 1 || window == null
                || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Coach rate configuration is invalid");
        }
        this.store = store;
        this.maximumRequests = maximumRequests;
        this.window = window;
    }

    public void assertAllowed(String ownerId) {
        if (ownerId == null || ownerId.isBlank()) throw new CoachRateLimitExceededException();
        try {
            if (store.incrementWithTtl(KEY_PREFIX + sha256(ownerId), window) > maximumRequests) {
                throw new CoachRateLimitExceededException();
            }
        } catch (EphemeralStateUnavailableException exception) {
            throw new CoachRateLimitExceededException();
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}

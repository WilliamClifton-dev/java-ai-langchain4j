package com.atguigu.java.ai.langchain4j.infrastructure.redis;

import java.time.Duration;
import java.util.Optional;

public final class UnavailableEphemeralStateStore implements EphemeralStateStore {
    @Override public long incrementWithTtl(String key, Duration ttl) { throw unavailable(); }
    @Override public long counter(String key) { throw unavailable(); }
    @Override public Optional<String> get(String key) { throw unavailable(); }
    @Override public void put(String key, String value, Duration ttl) { throw unavailable(); }
    @Override public boolean putIfAbsent(String key, String value, Duration ttl) {
        throw unavailable();
    }
    @Override public void delete(String key) { throw unavailable(); }

    private EphemeralStateUnavailableException unavailable() {
        return new EphemeralStateUnavailableException();
    }
}

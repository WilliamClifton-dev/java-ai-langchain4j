package com.atguigu.java.ai.langchain4j.infrastructure.redis;

import java.time.Duration;
import java.util.Optional;

public interface EphemeralStateStore {
    long incrementWithTtl(String key, Duration ttl);

    long counter(String key);

    Optional<String> get(String key);

    void put(String key, String value, Duration ttl);

    boolean putIfAbsent(String key, String value, Duration ttl);

    void delete(String key);
}

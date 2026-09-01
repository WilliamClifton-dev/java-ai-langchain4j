package com.atguigu.java.ai.langchain4j.infrastructure.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public final class RedisEphemeralStateStore implements EphemeralStateStore {
    private static final DefaultRedisScript<Long> INCREMENT_WITH_TTL =
            new DefaultRedisScript<>("""
                    local current = redis.call('INCR', KEYS[1])
                    if current == 1 then
                      redis.call('PEXPIRE', KEYS[1], ARGV[1])
                    end
                    return current
                    """, Long.class);
    private static final DefaultRedisScript<Long> DELETE_IF_VALUE =
            new DefaultRedisScript<>("""
                    if redis.call('GET', KEYS[1]) == ARGV[1] then
                      return redis.call('DEL', KEYS[1])
                    end
                    return 0
                    """, Long.class);

    private final StringRedisTemplate redis;

    public RedisEphemeralStateStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public long incrementWithTtl(String key, Duration ttl) {
        validate(key, ttl);
        try {
            Long value = redis.execute(
                    INCREMENT_WITH_TTL, List.of(key), Long.toString(ttl.toMillis()));
            if (value == null) throw new EphemeralStateUnavailableException();
            return value;
        } catch (EphemeralStateUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new EphemeralStateUnavailableException(exception);
        }
    }

    @Override
    public long counter(String key) {
        try {
            String value = redis.opsForValue().get(key);
            return value == null ? 0L : Long.parseLong(value);
        } catch (RuntimeException exception) {
            throw new EphemeralStateUnavailableException(exception);
        }
    }

    @Override
    public Optional<String> get(String key) {
        try {
            return Optional.ofNullable(redis.opsForValue().get(key));
        } catch (RuntimeException exception) {
            throw new EphemeralStateUnavailableException(exception);
        }
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        validate(key, ttl);
        try {
            redis.opsForValue().set(key, value, ttl);
        } catch (RuntimeException exception) {
            throw new EphemeralStateUnavailableException(exception);
        }
    }

    @Override
    public boolean putIfAbsent(String key, String value, Duration ttl) {
        validate(key, ttl);
        try {
            return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, value, ttl));
        } catch (RuntimeException exception) {
            throw new EphemeralStateUnavailableException(exception);
        }
    }

    @Override
    public boolean deleteIfValue(String key, String expectedValue) {
        if (key == null || key.isBlank() || expectedValue == null) return false;
        try {
            Long deleted = redis.execute(DELETE_IF_VALUE, List.of(key), expectedValue);
            return deleted != null && deleted == 1L;
        } catch (RuntimeException exception) {
            throw new EphemeralStateUnavailableException(exception);
        }
    }

    @Override
    public void delete(String key) {
        try {
            redis.delete(key);
        } catch (RuntimeException exception) {
            throw new EphemeralStateUnavailableException(exception);
        }
    }

    private void validate(String key, Duration ttl) {
        if (key == null || key.isBlank() || ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Ephemeral key or TTL is invalid");
        }
    }
}

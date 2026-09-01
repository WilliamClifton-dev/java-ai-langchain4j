package com.atguigu.java.ai.langchain4j.infrastructure.redis;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryEphemeralStateStore implements EphemeralStateStore {
    private final Clock clock;
    private final ConcurrentHashMap<String, Entry> values = new ConcurrentHashMap<>();

    public InMemoryEphemeralStateStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public synchronized long incrementWithTtl(String key, Duration ttl) {
        validate(key, ttl);
        Entry current = current(key).orElse(null);
        long next = current == null ? 1 : Long.parseLong(current.value()) + 1;
        Instant expiresAt = current == null ? clock.instant().plus(ttl) : current.expiresAt();
        values.put(key, new Entry(Long.toString(next), expiresAt));
        return next;
    }

    @Override
    public long counter(String key) {
        return current(key).map(entry -> Long.parseLong(entry.value())).orElse(0L);
    }

    @Override
    public Optional<String> get(String key) {
        return current(key).map(Entry::value);
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        validate(key, ttl);
        if (value == null) throw new IllegalArgumentException("Ephemeral value is required");
        values.put(key, new Entry(value, clock.instant().plus(ttl)));
    }

    @Override
    public synchronized boolean putIfAbsent(String key, String value, Duration ttl) {
        validate(key, ttl);
        if (value == null) throw new IllegalArgumentException("Ephemeral value is required");
        if (current(key).isPresent()) return false;
        values.put(key, new Entry(value, clock.instant().plus(ttl)));
        return true;
    }

    @Override
    public synchronized boolean deleteIfValue(String key, String expectedValue) {
        if (key == null || key.isBlank() || expectedValue == null) return false;
        Optional<Entry> current = current(key);
        if (current.isEmpty() || !current.get().value().equals(expectedValue)) return false;
        return values.remove(key, current.get());
    }

    @Override
    public void delete(String key) {
        if (key != null) values.remove(key);
    }

    public Set<String> keys() {
        values.keySet().forEach(this::current);
        return Set.copyOf(values.keySet());
    }

    private Optional<Entry> current(String key) {
        if (key == null || key.isBlank()) return Optional.empty();
        Entry entry = values.get(key);
        if (entry != null && !clock.instant().isBefore(entry.expiresAt())) {
            values.remove(key, entry);
            return Optional.empty();
        }
        return Optional.ofNullable(entry);
    }

    private void validate(String key, Duration ttl) {
        if (key == null || key.isBlank() || ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Ephemeral key or TTL is invalid");
        }
    }

    private record Entry(String value, Instant expiresAt) { }
}

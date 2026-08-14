package com.atguigu.java.ai.langchain4j.identity;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class LoginAttemptGuard {

    private static final int DEFAULT_MAXIMUM_FAILURES = 10;
    private static final Duration DEFAULT_WINDOW = Duration.ofMinutes(15);
    private static final int DEFAULT_MAXIMUM_TRACKED_KEYS = 10_000;

    private final Clock clock;
    private final int maximumFailures;
    private final Duration window;
    private final int maximumTrackedKeys;
    private final Map<String, AttemptWindow> attempts = new LinkedHashMap<>(16, 0.75f, true);

    @Autowired
    public LoginAttemptGuard(Clock clock) {
        this(clock, DEFAULT_MAXIMUM_FAILURES, DEFAULT_WINDOW, DEFAULT_MAXIMUM_TRACKED_KEYS);
    }

    LoginAttemptGuard(Clock clock, int maximumFailures, Duration window, int maximumTrackedKeys) {
        this.clock = clock;
        this.maximumFailures = maximumFailures;
        this.window = window;
        this.maximumTrackedKeys = maximumTrackedKeys;
    }

    public synchronized void assertAllowed(String key) {
        AttemptWindow current = currentWindow(key);
        if (current != null && current.failures() >= maximumFailures) {
            throw new TooManyLoginAttemptsException();
        }
    }

    public synchronized void recordFailure(String key) {
        AttemptWindow current = currentWindow(key);
        if (current == null) {
            evictEldestIfFull();
            attempts.put(key, new AttemptWindow(1, clock.instant()));
            return;
        }
        attempts.put(key, new AttemptWindow(current.failures() + 1, current.startedAt()));
    }

    public synchronized void recordSuccess(String key) {
        attempts.remove(key);
    }

    public static String key(String remoteAddress, String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String source = (remoteAddress == null ? "" : remoteAddress) + '|' + normalizedEmail;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private AttemptWindow currentWindow(String key) {
        AttemptWindow current = attempts.get(key);
        if (current != null && !clock.instant().isBefore(current.startedAt().plus(window))) {
            attempts.remove(key);
            return null;
        }
        return current;
    }

    private void evictEldestIfFull() {
        if (attempts.size() < maximumTrackedKeys) {
            return;
        }
        String eldest = attempts.keySet().iterator().next();
        attempts.remove(eldest);
    }

    private record AttemptWindow(int failures, Instant startedAt) {
    }
}

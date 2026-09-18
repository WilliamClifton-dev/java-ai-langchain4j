package com.atguigu.java.ai.langchain4j.identity;

import org.springframework.stereotype.Component;
import com.atguigu.java.ai.langchain4j.infrastructure.redis.EphemeralStateStore;
import com.atguigu.java.ai.langchain4j.infrastructure.redis.EphemeralStateUnavailableException;
import com.atguigu.java.ai.langchain4j.infrastructure.redis.InMemoryEphemeralStateStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class LoginAttemptGuard {

    private static final int DEFAULT_MAXIMUM_FAILURES = 10;
    private static final Duration DEFAULT_WINDOW = Duration.ofMinutes(15);
    private static final String KEY_PREFIX = "rate:login:";
    private final EphemeralStateStore store;
    private final int maximumFailures;
    private final Duration window;

    @Autowired
    public LoginAttemptGuard(EphemeralStateStore store,
                             @Value("${hbti.rate.login.maximum-failures:10}") int maximumFailures,
                             @Value("${hbti.rate.login.window:PT15M}") Duration window) {
        if (store == null || maximumFailures < 1 || window == null
                || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Login rate configuration is invalid");
        }
        this.store = store;
        this.maximumFailures = maximumFailures;
        this.window = window;
    }

    LoginAttemptGuard(Clock clock, int maximumFailures, Duration window, int maximumTrackedKeys) {
        this(new InMemoryEphemeralStateStore(clock), maximumFailures, window);
    }

    public void assertAllowed(String key) {
        try {
            if (store.counter(KEY_PREFIX + key) >= maximumFailures) {
                throw new TooManyLoginAttemptsException();
            }
        } catch (EphemeralStateUnavailableException exception) {
            throw new TooManyLoginAttemptsException();
        }
    }

    public void recordFailure(String key) {
        try {
            store.incrementWithTtl(KEY_PREFIX + key, window);
        } catch (EphemeralStateUnavailableException ignored) {
            // The next admission check fails closed while shared state is unavailable.
        }
    }

    public void recordSuccess(String key) {
        try {
            store.delete(KEY_PREFIX + key);
        } catch (EphemeralStateUnavailableException ignored) {
            // Authentication success remains valid; the bounded key expires naturally.
        }
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

}

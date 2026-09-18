package com.atguigu.java.ai.langchain4j.infrastructure.redis;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

@Component
public class RequestLeaseCoordinator {
    private static final Duration MIN_TTL = Duration.ofSeconds(1);
    private static final Duration MAX_TTL = Duration.ofMinutes(5);
    private static final int MAX_OWNER_BYTES = 128;
    private static final int MAX_IDEMPOTENCY_KEY_BYTES = 128;
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9-]{1,32}");

    private final EphemeralStateStore store;

    public RequestLeaseCoordinator(EphemeralStateStore store) {
        this.store = store;
    }

    public Lease acquire(String namespace, String owner, String idempotencyKey, Duration ttl) {
        validate(namespace, owner, idempotencyKey, ttl);
        String key = "lease:" + namespace + ":v1:" + sha256(owner + "\0" + idempotencyKey);
        String tokenDigest = sha256(UUID.randomUUID().toString());
        try {
            if (!store.putIfAbsent(key, tokenDigest, ttl)) {
                throw new RequestAlreadyInFlightException();
            }
            return new Lease(store, key, tokenDigest, true);
        } catch (EphemeralStateUnavailableException exception) {
            // Durable MySQL idempotency remains authoritative when optional coordination is down.
            return new Lease(null, null, null, false);
        }
    }

    private void validate(String namespace, String owner, String idempotencyKey, Duration ttl) {
        if (namespace == null || !NAMESPACE.matcher(namespace).matches()
                || owner == null || owner.isBlank()
                || owner.getBytes(StandardCharsets.UTF_8).length > MAX_OWNER_BYTES
                || idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.getBytes(StandardCharsets.UTF_8).length > MAX_IDEMPOTENCY_KEY_BYTES
                || ttl == null || ttl.compareTo(MIN_TTL) < 0 || ttl.compareTo(MAX_TTL) > 0) {
            throw new IllegalArgumentException("Request lease input is invalid");
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public static final class Lease implements AutoCloseable {
        private final EphemeralStateStore store;
        private final String key;
        private final String tokenDigest;
        private final boolean coordinated;
        private final AtomicBoolean closed = new AtomicBoolean();

        private Lease(EphemeralStateStore store, String key, String tokenDigest, boolean coordinated) {
            this.store = store;
            this.key = key;
            this.tokenDigest = tokenDigest;
            this.coordinated = coordinated;
        }

        public boolean coordinated() {
            return coordinated;
        }

        @Override
        public void close() {
            if (!coordinated || !closed.compareAndSet(false, true)) return;
            try {
                store.deleteIfValue(key, tokenDigest);
            } catch (EphemeralStateUnavailableException ignored) {
                // The bounded TTL releases the lease if Redis fails during cleanup.
            }
        }
    }
}

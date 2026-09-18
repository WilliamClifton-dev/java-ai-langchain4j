package com.atguigu.java.ai.langchain4j.infrastructure.redis;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RedisIdempotencyPolicyTest {

    @Test
    void rejectsADuplicateLeaseWithoutPuttingRawIdentityInRedis() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-15T00:00:00Z"));
        InMemoryEphemeralStateStore store = new InMemoryEphemeralStateStore(clock);
        RequestLeaseCoordinator coordinator = new RequestLeaseCoordinator(store);

        RequestLeaseCoordinator.Lease lease = coordinator.acquire(
                "assessment", "owner-sensitive", "request-sensitive", Duration.ofSeconds(30));

        assertThat(lease.coordinated()).isTrue();
        assertThatThrownBy(() -> coordinator.acquire(
                "assessment", "owner-sensitive", "request-sensitive", Duration.ofSeconds(30)))
                .isInstanceOf(RequestAlreadyInFlightException.class);
        assertThat(store.keys()).allMatch(key -> key.startsWith("lease:assessment:v1:"))
                .noneMatch(key -> key.contains("owner-sensitive") || key.contains("request-sensitive"));
    }

    @Test
    void anExpiredLeaseCannotDeleteItsReplacement() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-15T00:00:00Z"));
        InMemoryEphemeralStateStore store = new InMemoryEphemeralStateStore(clock);
        RequestLeaseCoordinator coordinator = new RequestLeaseCoordinator(store);
        RequestLeaseCoordinator.Lease first = coordinator.acquire(
                "plan", "owner", "request", Duration.ofSeconds(1));

        clock.advance(Duration.ofSeconds(2));
        RequestLeaseCoordinator.Lease replacement = coordinator.acquire(
                "plan", "owner", "request", Duration.ofSeconds(30));
        first.close();

        assertThatThrownBy(() -> coordinator.acquire(
                "plan", "owner", "request", Duration.ofSeconds(30)))
                .isInstanceOf(RequestAlreadyInFlightException.class);
        replacement.close();
        assertThat(coordinator.acquire(
                "plan", "owner", "request", Duration.ofSeconds(30)).coordinated()).isTrue();
    }

    @Test
    void boundsLeaseLifetime() {
        RequestLeaseCoordinator coordinator = new RequestLeaseCoordinator(
                new InMemoryEphemeralStateStore(Clock.systemUTC()));

        assertThatThrownBy(() -> coordinator.acquire(
                "plan", "owner", "request", Duration.ofMillis(999)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> coordinator.acquire(
                "plan", "owner", "request", Duration.ofMinutes(6)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> coordinator.acquire(
                "plan", "owner", "x".repeat(129), Duration.ofSeconds(30)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}

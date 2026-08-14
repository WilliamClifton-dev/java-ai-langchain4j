package com.atguigu.java.ai.langchain4j.coach.streaming;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ModelCircuitBreakerTest {

    @Test
    void opensAfterBoundedFailuresAndAllowsOneProbeAfterCooldown() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-15T00:00:00Z"));
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(2, Duration.ofSeconds(30), clock);

        ModelCircuitBreaker.Permit first = breaker.tryAcquire();
        assertThat(first.allowed()).isTrue();
        breaker.onFailure(first);

        ModelCircuitBreaker.Permit second = breaker.tryAcquire();
        assertThat(second.allowed()).isTrue();
        breaker.onFailure(second);

        assertThat(breaker.tryAcquire().allowed()).isFalse();

        clock.advance(Duration.ofSeconds(30));
        ModelCircuitBreaker.Permit probe = breaker.tryAcquire();
        assertThat(probe.allowed()).isTrue();
        assertThat(probe.probe()).isTrue();
        assertThat(breaker.tryAcquire().allowed()).isFalse();

        breaker.onSuccess(probe);
        ModelCircuitBreaker.Permit recovered = breaker.tryAcquire();
        assertThat(recovered.allowed()).isTrue();
        assertThat(recovered.probe()).isFalse();
    }

    @Test
    void failedProbeReopensForAnotherCooldown() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-15T00:00:00Z"));
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(1, Duration.ofSeconds(10), clock);
        ModelCircuitBreaker.Permit failure = breaker.tryAcquire();
        breaker.onFailure(failure);

        clock.advance(Duration.ofSeconds(10));
        ModelCircuitBreaker.Permit probe = breaker.tryAcquire();
        breaker.onFailure(probe);

        assertThat(breaker.tryAcquire().allowed()).isFalse();
        clock.advance(Duration.ofSeconds(10));
        assertThat(breaker.tryAcquire().allowed()).isTrue();
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
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}

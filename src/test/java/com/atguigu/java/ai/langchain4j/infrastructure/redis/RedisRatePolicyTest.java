package com.atguigu.java.ai.langchain4j.infrastructure.redis;

import com.atguigu.java.ai.langchain4j.coach.streaming.CoachRateGuard;
import com.atguigu.java.ai.langchain4j.coach.streaming.CoachRateLimitExceededException;
import com.atguigu.java.ai.langchain4j.identity.LoginAttemptGuard;
import com.atguigu.java.ai.langchain4j.identity.TooManyLoginAttemptsException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RedisRatePolicyTest {

    @Test
    void loginFailuresUseAnExpiringDigestKeyAndResetAfterSuccess() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-15T00:00:00Z"), ZoneOffset.UTC);
        InMemoryEphemeralStateStore store = new InMemoryEphemeralStateStore(clock);
        LoginAttemptGuard guard = new LoginAttemptGuard(store, 2, Duration.ofMinutes(15));
        String digest = LoginAttemptGuard.key("192.0.2.10", "USER@example.com");

        guard.recordFailure(digest);
        guard.recordFailure(digest);

        assertThatThrownBy(() -> guard.assertAllowed(digest))
                .isInstanceOf(TooManyLoginAttemptsException.class);
        assertThat(store.keys()).allMatch(key -> key.startsWith("rate:login:"))
                .noneMatch(key -> key.contains("192.0.2.10") || key.contains("example.com"));
        guard.recordSuccess(digest);
        assertThatCode(() -> guard.assertAllowed(digest)).doesNotThrowAnyException();
    }

    @Test
    void coachConsumptionIsBoundedAndNeverUsesTheRawOwnerAsARedisKey() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-15T00:00:00Z"), ZoneOffset.UTC);
        InMemoryEphemeralStateStore store = new InMemoryEphemeralStateStore(clock);
        CoachRateGuard guard = new CoachRateGuard(store, 2, Duration.ofMinutes(1));

        guard.assertAllowed("owner-sensitive-id");
        guard.assertAllowed("owner-sensitive-id");

        assertThatThrownBy(() -> guard.assertAllowed("owner-sensitive-id"))
                .isInstanceOf(CoachRateLimitExceededException.class);
        assertThat(store.keys()).allMatch(key -> key.startsWith("rate:coach:"))
                .noneMatch(key -> key.contains("owner-sensitive-id"));
    }

    @Test
    void securitySensitiveAdmissionFailsClosedWhenTheStoreIsUnavailable() {
        EphemeralStateStore unavailable = new UnavailableEphemeralStateStore();
        LoginAttemptGuard login = new LoginAttemptGuard(
                unavailable, 10, Duration.ofMinutes(15));
        CoachRateGuard coach = new CoachRateGuard(unavailable, 20, Duration.ofMinutes(1));

        assertThatThrownBy(() -> login.assertAllowed("digest"))
                .isInstanceOf(TooManyLoginAttemptsException.class);
        assertThatThrownBy(() -> coach.assertAllowed("owner"))
                .isInstanceOf(CoachRateLimitExceededException.class);
    }
}

package com.atguigu.java.ai.langchain4j.identity;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptGuardTest {

    @Test
    void blocksAKeyAfterTheConfiguredNumberOfFailuresAndResetsOnSuccess() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-14T08:00:00Z"), ZoneOffset.UTC);
        LoginAttemptGuard guard = new LoginAttemptGuard(clock, 3, Duration.ofMinutes(15), 100);

        guard.recordFailure("ip:email");
        guard.recordFailure("ip:email");
        guard.recordFailure("ip:email");

        assertThatThrownBy(() -> guard.assertAllowed("ip:email"))
                .isInstanceOf(TooManyLoginAttemptsException.class);
        guard.recordSuccess("ip:email");
        assertThatCode(() -> guard.assertAllowed("ip:email")).doesNotThrowAnyException();
    }
}

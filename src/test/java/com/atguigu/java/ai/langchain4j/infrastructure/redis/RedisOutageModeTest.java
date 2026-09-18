package com.atguigu.java.ai.langchain4j.infrastructure.redis;

import com.atguigu.java.ai.langchain4j.assessment.CachedHbtiDefinitionCatalog;
import com.atguigu.java.ai.langchain4j.assessment.HbtiDefinition;
import com.atguigu.java.ai.langchain4j.assessment.HbtiDefinitionRepository;
import com.atguigu.java.ai.langchain4j.coach.streaming.CoachRateGuard;
import com.atguigu.java.ai.langchain4j.coach.streaming.CoachRateLimitExceededException;
import com.atguigu.java.ai.langchain4j.identity.LoginAttemptGuard;
import com.atguigu.java.ai.langchain4j.identity.TooManyLoginAttemptsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisOutageModeTest {

    @Test
    void failsClosedForAdmissionButBypassesOptionalCoordinationAndCache() {
        UnavailableEphemeralStateStore unavailable = new UnavailableEphemeralStateStore();
        LoginAttemptGuard login = new LoginAttemptGuard(unavailable, 10, Duration.ofMinutes(15));
        CoachRateGuard coach = new CoachRateGuard(unavailable, 20, Duration.ofMinutes(1));
        RequestLeaseCoordinator coordinator = new RequestLeaseCoordinator(unavailable);
        HbtiDefinitionRepository source = mock(HbtiDefinitionRepository.class);
        HbtiDefinition definition = ReconstructableCacheTest.definition();
        when(source.findPublished("hbti", "1.0.0")).thenReturn(Optional.of(definition));
        CachedHbtiDefinitionCatalog catalog = new CachedHbtiDefinitionCatalog(
                source, unavailable, new ObjectMapper().findAndRegisterModules());

        assertThatThrownBy(() -> login.assertAllowed("digest"))
                .isInstanceOf(TooManyLoginAttemptsException.class);
        assertThatThrownBy(() -> coach.assertAllowed("owner"))
                .isInstanceOf(CoachRateLimitExceededException.class);
        assertThat(coordinator.acquire(
                "assessment", "owner", "request", Duration.ofSeconds(30)).coordinated())
                .isFalse();
        assertThat(catalog.findPublished("hbti", "1.0.0")).contains(definition);
    }
}

package com.atguigu.java.ai.langchain4j.coach.service;

import com.atguigu.java.ai.langchain4j.assistant.HbtiCoachAgent;
import com.atguigu.java.ai.langchain4j.coach.dto.CoachChatCommand;
import com.atguigu.java.ai.langchain4j.coach.model.CoachScene;
import com.atguigu.java.ai.langchain4j.coach.streaming.*;
import com.atguigu.java.ai.langchain4j.store.CoachConversationOwnershipService;
import com.atguigu.java.ai.langchain4j.coach.tool.CoachToolContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import com.atguigu.java.ai.langchain4j.config.CoachStreamingConfig;
import com.atguigu.java.ai.langchain4j.coach.prompt.ScenePromptRepository;
import com.atguigu.java.ai.langchain4j.infrastructure.redis.InMemoryEphemeralStateStore;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(SharedCoachProtectionTest.Config.class)
@TestPropertySource(properties = {"hbti.rate.coach.maximum-requests=2",
        "hbti.coach.streaming.max-concurrent=1", "hbti.coach.streaming.circuit-failure-threshold=1"})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SharedCoachProtectionTest {
    @Autowired CoachChatService chat;
    @Autowired CoachStreamingService streaming;
    @MockBean HbtiCoachAgent agent;
    @MockBean CoachStreamingModel model;
    @MockBean CoachConversationOwnershipService ownership;

    @TestConfiguration
    @Import({CoachChatService.class, CoachStreamingConfig.class, ScenePromptRepository.class, CoachToolContext.class})
    static class Config {
        @Bean static org.springframework.core.convert.ConversionService conversionService() {
            return org.springframework.boot.convert.ApplicationConversionService.getSharedInstance();
        }
        @Bean Clock clock() { return Clock.systemUTC(); }
        @Bean InMemoryEphemeralStateStore state(Clock clock) { return new InMemoryEphemeralStateStore(clock); }
        @Bean CoachMetrics metrics() { return new CoachMetrics(new SimpleMeterRegistry()); }
    }

    @Test
    void synchronousAndStreamingRequestsSpendTheSameOwnerQuota() {
        streaming.open(command(), mock(CoachEventSink.class)).cancel();
        streaming.open(command(), mock(CoachEventSink.class)).cancel();
        assertThatThrownBy(() -> chat.chat(command())).hasMessageContaining("MODEL_RATE_LIMITED");
        verifyNoInteractions(agent);
    }

    @Test
    void activeStreamBlocksSynchronousCallsUntilCancellationReleasesCapacity() {
        var stream = streaming.open(command(), mock(CoachEventSink.class));
        assertThatThrownBy(() -> chat.chat(otherOwner())).hasMessageContaining("MODEL_CONCURRENCY_LIMIT");
        verifyNoInteractions(agent);
        stream.cancel();
        when(agent.chat(any(), any(), any(), any())).thenReturn("answer");
        assertThat(chat.chat(otherOwner()).answer()).isEqualTo("answer");
    }

    @Test
    void providerFailuresInSynchronousCallsOpenTheStreamingCircuit() {
        when(agent.chat(any(), any(), any(), any())).thenThrow(new IllegalStateException("secret"));
        assertThatThrownBy(() -> chat.chat(command())).hasMessageContaining("MODEL_UNAVAILABLE");
        CoachEventSink sink = mock(CoachEventSink.class);
        streaming.open(otherOwner(), sink);
        verify(sink).error("MODEL_CIRCUIT_OPEN", true);
        verifyNoInteractions(model);
    }

    private CoachChatCommand command() {
        return new CoachChatCommand("shared-quota-owner", "c1", CoachScene.GENERAL_CHAT, "hello");
    }

    private CoachChatCommand otherOwner() {
        return new CoachChatCommand("another-owner", "c2", CoachScene.GENERAL_CHAT, "hello");
    }
}

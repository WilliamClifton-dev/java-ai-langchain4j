package com.atguigu.java.ai.langchain4j.config;

import com.atguigu.java.ai.langchain4j.coach.service.CoachChatService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthProbeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean(name = "redisHealthContributor")
    private ReactiveHealthIndicator redisHealthIndicator;

    @MockBean
    private CoachChatService coachChatService;

    @MockBean(name = "chatModel")
    private ChatLanguageModel chatModel;

    @BeforeEach
    void redisIsAvailable() {
        Mono<Health> available = Mono.just(Health.up().build());
        when(redisHealthIndicator.health()).thenReturn(available);
        when(redisHealthIndicator.getHealth(anyBoolean())).thenReturn(available);
    }

    @Test
    void livenessIsPublicAndExcludesExternalDependencies() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.livenessState.status").value("UP"))
                .andExpect(jsonPath("$.components.database").doesNotExist())
                .andExpect(jsonPath("$.components.redis").doesNotExist());
    }

    @Test
    void readinessIsPublicAndIncludesMysqlAndRedis() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.readinessState.status").value("UP"))
                .andExpect(jsonPath("$.components.database.status").value("UP"))
                .andExpect(jsonPath("$.components.redis.status").value("UP"));
    }

    @Test
    void regularAuthenticatedUserCannotReadDependencyDiagnosticDetails() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.database.status").value("UP"))
                .andExpect(jsonPath("$.components.database.details").doesNotExist());
    }

    @Test
    void redisOutageRemovesReadinessWithoutFailingLiveness() throws Exception {
        Mono<Health> unavailable = Mono.just(Health.down().build());
        when(redisHealthIndicator.health()).thenReturn(unavailable);
        when(redisHealthIndicator.getHealth(anyBoolean())).thenReturn(unavailable);

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.components.database.status").value("UP"))
                .andExpect(jsonPath("$.components.redis.status").value("DOWN"));
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.redis").doesNotExist());
    }
}

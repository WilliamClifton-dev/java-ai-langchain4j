package com.atguigu.java.ai.langchain4j.tracking.api;

import com.atguigu.java.ai.langchain4j.coach.service.CoachChatService;
import com.atguigu.java.ai.langchain4j.identity.AccountRegistrationService;
import com.atguigu.java.ai.langchain4j.identity.RegisterAccountCommand;
import com.atguigu.java.ai.langchain4j.profile.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DailyTrackingApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AccountRegistrationService registration;
    @Autowired ProfileService profiles;
    @MockBean CoachChatService coachChatService;
    @MockBean(name = "chatModel") ChatLanguageModel chatModel;

    @Test
    void recordsOwnedTypedFactsAndReturnsTheDailySummary() throws Exception {
        String userId = user();
        LocalDate date = LocalDate.now();
        String metric = json.writeValueAsString(Map.of(
                "localDate", date.toString(), "weightKg", 70.2, "steps", 8000,
                "activityMinutes", 45, "sleepMinutes", 450, "sleepQuality", 4,
                "userId", "ignored"
        ));
        mvc.perform(post("/api/v1/tracking/daily-metrics").with(jwt().jwt(j -> j.subject(userId)))
                        .with(csrf()).header("Idempotency-Key", "api-metric")
                        .contentType(MediaType.APPLICATION_JSON).content(metric))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.replayed").value(false));
        mvc.perform(post("/api/v1/tracking/daily-metrics").with(jwt().jwt(j -> j.subject(userId)))
                        .with(csrf()).header("Idempotency-Key", "api-metric")
                        .contentType(MediaType.APPLICATION_JSON).content(metric))
                .andExpect(status().isOk()).andExpect(jsonPath("$.replayed").value(true));
        mvc.perform(post("/api/v1/tracking/training").with(jwt().jwt(j -> j.subject(userId)))
                        .with(csrf()).header("Idempotency-Key", "api-training")
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "localDate", date.toString(), "trainingType", "STRENGTH",
                                "durationMinutes", 60, "intensity", "HIGH"))))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/tracking/nutrition").with(jwt().jwt(j -> j.subject(userId)))
                        .with(csrf()).header("Idempotency-Key", "api-nutrition")
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "localDate", date.toString(), "energyKcal", 2050,
                                "proteinG", 125.5, "carbohydrateG", 220.0, "fatG", 65.0))))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/tracking/days/{date}", date)
                        .with(jwt().jwt(j -> j.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metric.weightKg").value(70.2))
                .andExpect(jsonPath("$.nutrition.energyKcal").value(2050))
                .andExpect(jsonPath("$.trainingMinutes").value(60));
    }

    @Test
    void returnsStableErrorsForMissingKeysAndDuplicateDailyFacts() throws Exception {
        String userId = user("tracking-api-errors@example.com");
        LocalDate date = LocalDate.now();
        String metric = json.writeValueAsString(Map.of(
                "localDate", date.toString(), "steps", 5000
        ));

        mvc.perform(post("/api/v1/tracking/daily-metrics").with(jwt().jwt(j -> j.subject(userId)))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(metric))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_TRACKING_REQUEST"));

        mvc.perform(post("/api/v1/tracking/daily-metrics").with(jwt().jwt(j -> j.subject(userId)))
                        .with(csrf()).header("Idempotency-Key", "first-key")
                        .contentType(MediaType.APPLICATION_JSON).content(metric))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.record.userId").doesNotExist());

        mvc.perform(post("/api/v1/tracking/daily-metrics").with(jwt().jwt(j -> j.subject(userId)))
                        .with(csrf()).header("Idempotency-Key", "second-key")
                        .contentType(MediaType.APPLICATION_JSON).content(metric))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("TRACKING_DATE_CONFLICT"));
    }

    private String user() {
        return user("tracking-api@example.com");
    }

    private String user(String email) {
        String id = registration.register(new RegisterAccountCommand(
                email, "correct horse battery staple")).id();
        profiles.save(id, new SaveProfileCommand(LocalDate.of(1990, 1, 1),
                CalculationSex.FEMALE, 165, 70, 60, ActivityLevel.MODERATE, "Asia/Hong_Kong"));
        return id;
    }
}

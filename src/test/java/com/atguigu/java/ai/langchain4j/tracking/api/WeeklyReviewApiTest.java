package com.atguigu.java.ai.langchain4j.tracking.api;

import com.atguigu.java.ai.langchain4j.assessment.HbtiAnswer;
import com.atguigu.java.ai.langchain4j.assessment.HbtiAssessmentService;
import com.atguigu.java.ai.langchain4j.assessment.SubmitHbtiAssessmentCommand;
import com.atguigu.java.ai.langchain4j.coach.service.CoachChatService;
import com.atguigu.java.ai.langchain4j.identity.AccountRegistrationService;
import com.atguigu.java.ai.langchain4j.identity.RegisterAccountCommand;
import com.atguigu.java.ai.langchain4j.planning.WeightGoal;
import com.atguigu.java.ai.langchain4j.planning.WeightPlanService;
import com.atguigu.java.ai.langchain4j.planning.WeightPlanVersion;
import com.atguigu.java.ai.langchain4j.profile.*;
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
import java.util.List;
import java.util.stream.IntStream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WeeklyReviewApiTest {
    @Autowired MockMvc mvc;
    @Autowired AccountRegistrationService registration;
    @Autowired ProfileService profiles;
    @Autowired HbtiAssessmentService assessments;
    @Autowired WeightPlanService plans;
    @MockBean CoachChatService coachChatService;
    @MockBean(name = "chatModel") ChatLanguageModel chatModel;

    @Test
    void createsReplaysAndReadsAnOwnedWeeklyReview() throws Exception {
        String userId = activeUser();
        String body = "{\"windowEnd\":\"" + LocalDate.now() + "\"}";

        String response = mvc.perform(post("/api/v1/tracking/weekly-reviews")
                        .with(jwt().jwt(j -> j.subject(userId))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.replayed").value(false))
                .andExpect(jsonPath("$.review.recommendation").value("INSUFFICIENT_DATA"))
                .andExpect(jsonPath("$.review.limitation").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String reviewId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response).path("review").path("id").asText();

        mvc.perform(post("/api/v1/tracking/weekly-reviews")
                        .with(jwt().jwt(j -> j.subject(userId))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.replayed").value(true));
        mvc.perform(get("/api/v1/tracking/weekly-reviews/{id}", reviewId)
                        .with(jwt().jwt(j -> j.subject(userId))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(reviewId));
    }

    private String activeUser() {
        String userId = registration.register(new RegisterAccountCommand(
                "weekly-api@example.com", "correct horse battery staple")).id();
        profiles.save(userId, new SaveProfileCommand(LocalDate.of(1990, 1, 1),
                CalculationSex.FEMALE, 165, 70, 60, ActivityLevel.MODERATE, "Asia/Hong_Kong"));
        profiles.screen(userId, new SafetyScreeningAnswers(false, false, false, false, false));
        List<HbtiAnswer> answers = IntStream.rangeClosed(1, 16)
                .mapToObj(index -> new HbtiAnswer("q" + index, 3)).toList();
        assessments.submit(userId, "weekly-api-assessment",
                new SubmitHbtiAssessmentCommand("1.0.0", answers));
        WeightPlanVersion draft = plans.createDraft(userId, "weekly-api-draft", WeightGoal.LOSS);
        plans.validate(userId, draft.planId(), draft.id());
        plans.confirm(userId, draft.planId(), draft.id());
        plans.activate(userId, draft.planId(), draft.id(), "weekly-api-activation");
        return userId;
    }
}

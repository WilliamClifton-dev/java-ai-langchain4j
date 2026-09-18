package com.atguigu.java.ai.langchain4j.assessment.api;

import com.atguigu.java.ai.langchain4j.coach.service.CoachChatService;
import com.atguigu.java.ai.langchain4j.identity.AccountRegistrationService;
import com.atguigu.java.ai.langchain4j.identity.RegisterAccountCommand;
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

import java.util.List;
import java.util.Map;
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
class HbtiAssessmentApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRegistrationService registrationService;

    @MockBean
    private CoachChatService coachChatService;

    @MockBean(name = "chatModel")
    private ChatLanguageModel chatModel;

    @Test
    void readsPublishedQuestionnaireWithoutExposingScoringKeys() throws Exception {
        String userId = user("assessment-definition-api@example.com");

        mockMvc.perform(get("/api/v1/assessments/hbti/definitions/{version}", "1.0.0")
                        .with(jwt().jwt(token -> token.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.answerMin").value(1))
                .andExpect(jsonPath("$.answerMax").value(5))
                .andExpect(jsonPath("$.dimensions.length()").value(4))
                .andExpect(jsonPath("$.items.length()").value(16))
                .andExpect(jsonPath("$.items[0].itemKey").value("q1"))
                .andExpect(jsonPath("$.items[0].titleZh").isNotEmpty())
                .andExpect(jsonPath("$.items[0].targetPole").doesNotExist())
                .andExpect(jsonPath("$.sourceCommit").doesNotExist());
    }

    @Test
    void submitsReplaysAndReadsOwnedNonDiagnosticResults() throws Exception {
        String ownerId = user("assessment-api@example.com");
        String otherId = user("assessment-api-other@example.com");
        String request = requestBody(otherId);

        mockMvc.perform(post("/api/v1/assessments/hbti/submissions")
                        .with(jwt().jwt(token -> token.subject(ownerId)))
                        .with(csrf())
                        .header("Idempotency-Key", "api-request-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.replayed").value(false))
                .andExpect(jsonPath("$.result.typeCode").value("FHRN"))
                .andExpect(jsonPath("$.result.dimensions[0].leftScore").value(50))
                .andExpect(jsonPath("$.result.limitation").value(
                        "HBTI is an exploratory behavioral tendency assessment, not a diagnosis."
                ));

        mockMvc.perform(post("/api/v1/assessments/hbti/submissions")
                        .with(jwt().jwt(token -> token.subject(ownerId)))
                        .with(csrf())
                        .header("Idempotency-Key", "api-request-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").value(true));

        mockMvc.perform(get("/api/v1/assessments/hbti/results/current")
                        .with(jwt().jwt(token -> token.subject(ownerId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeCode").value("FHRN"));

        mockMvc.perform(get("/api/v1/assessments/hbti/results")
                        .with(jwt().jwt(token -> token.subject(ownerId)))
                        .param("page", "0").param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].typeCode").value("FHRN"));

        mockMvc.perform(get("/api/v1/assessments/hbti/results/current")
                        .with(jwt().jwt(token -> token.subject(otherId))))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsStableErrorsForConflictingIdempotencyAndInvalidAnswers() throws Exception {
        String userId = user("assessment-api-errors@example.com");
        mockMvc.perform(post("/api/v1/assessments/hbti/submissions")
                        .with(jwt().jwt(token -> token.subject(userId)))
                        .with(csrf())
                        .header("Idempotency-Key", "api-request-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(null)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/assessments/hbti/submissions")
                        .with(jwt().jwt(token -> token.subject(userId)))
                        .with(csrf())
                        .header("Idempotency-Key", "api-request-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyWithFirstAnswer(4)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_CONFLICT"));

        mockMvc.perform(post("/api/v1/assessments/hbti/submissions")
                        .with(jwt().jwt(token -> token.subject(userId)))
                        .with(csrf())
                        .header("Idempotency-Key", "api-request-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyWithAnswerCount(15)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ASSESSMENT_ANSWERS"));

        mockMvc.perform(post("/api/v1/assessments/hbti/submissions")
                        .with(jwt().jwt(token -> token.subject(userId)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ASSESSMENT_REQUEST"));
    }

    private String user(String email) {
        return registrationService.register(new RegisterAccountCommand(
                email, "correct horse battery staple"
        )).id();
    }

    private String requestBody(String ignoredClientUserId) throws Exception {
        return requestBodyWithFirstAnswer(3, ignoredClientUserId);
    }

    private String requestBodyWithFirstAnswer(int firstValue) throws Exception {
        return requestBodyWithFirstAnswer(firstValue, null);
    }

    private String requestBodyWithFirstAnswer(int firstValue, String ignoredClientUserId) throws Exception {
        List<Map<String, Object>> answers = answers(16, firstValue);
        Map<String, Object> request = new java.util.LinkedHashMap<>();
        request.put("definitionVersion", "1.0.0");
        request.put("answers", answers);
        if (ignoredClientUserId != null) {
            request.put("userId", ignoredClientUserId);
        }
        return objectMapper.writeValueAsString(request);
    }

    private String requestBodyWithAnswerCount(int answerCount) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "definitionVersion", "1.0.0",
                "answers", answers(answerCount, 3)
        ));
    }

    private List<Map<String, Object>> answers(int answerCount, int firstValue) {
        return IntStream.rangeClosed(1, answerCount)
                .mapToObj(index -> Map.<String, Object>of(
                        "itemKey", "q" + index,
                        "value", index == 1 ? firstValue : 3
                )).toList();
    }
}

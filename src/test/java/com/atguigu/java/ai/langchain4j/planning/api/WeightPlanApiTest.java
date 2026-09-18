package com.atguigu.java.ai.langchain4j.planning.api;

import com.atguigu.java.ai.langchain4j.assessment.HbtiAnswer;
import com.atguigu.java.ai.langchain4j.assessment.HbtiAssessmentService;
import com.atguigu.java.ai.langchain4j.assessment.SubmitHbtiAssessmentCommand;
import com.atguigu.java.ai.langchain4j.coach.service.CoachChatService;
import com.atguigu.java.ai.langchain4j.identity.AccountRegistrationService;
import com.atguigu.java.ai.langchain4j.identity.RegisterAccountCommand;
import com.atguigu.java.ai.langchain4j.profile.ActivityLevel;
import com.atguigu.java.ai.langchain4j.profile.CalculationSex;
import com.atguigu.java.ai.langchain4j.profile.ProfileService;
import com.atguigu.java.ai.langchain4j.profile.SafetyScreeningAnswers;
import com.atguigu.java.ai.langchain4j.profile.SaveProfileCommand;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WeightPlanApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRegistrationService registrationService;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private HbtiAssessmentService assessmentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private CoachChatService coachChatService;

    @MockBean(name = "chatModel")
    private ChatLanguageModel chatModel;

    @Test
    void createsAndActivatesAPlanUsingOnlyTheJwtOwner() throws Exception {
        String ownerId = eligibleUser("plan-api@example.com");
        String otherId = eligibleUser("plan-api-other@example.com");
        String draftJson = mockMvc.perform(post("/api/v1/plans/drafts")
                        .with(jwt().jwt(token -> token.subject(ownerId)))
                        .with(csrf())
                        .header("Idempotency-Key", "plan-api-draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "goal", "LOSS", "userId", otherId,
                                "energyMinKcalPerDay", 1
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.formulaVersion").value("MIFFLIN_ST_JEOR_METRIC_V1"))
                .andExpect(jsonPath("$.guidance").value(
                        "Targets are planning estimates, not medical prescriptions or guaranteed outcomes."
                ))
                .andReturn().getResponse().getContentAsString();
        JsonNode draft = objectMapper.readTree(draftJson);
        String planId = draft.get("planId").asText();
        String versionId = draft.get("id").asText();

        transition(ownerId, planId, versionId, "validation", "VALIDATED");
        transition(ownerId, planId, versionId, "confirmation", "CONFIRMED");
        transition(ownerId, planId, versionId, "activation", "ACTIVE");

        Integer activationAudits = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_event WHERE event_type = 'PLAN_ACTIVATED' "
                        + "AND user_id = ? AND details LIKE ?",
                Integer.class, ownerId, "%" + versionId + "%");
        assertThat(activationAudits).isEqualTo(1);

        mockMvc.perform(get("/api/v1/plans/active")
                        .with(jwt().jwt(token -> token.subject(ownerId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(versionId));
        mockMvc.perform(get("/api/v1/plans/{planId}/versions/{versionId}", planId, versionId)
                        .with(jwt().jwt(token -> token.subject(otherId))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PLAN_VERSION_NOT_FOUND"));
    }

    @Test
    void rejectsTransitionSkipsWithAStableConflict() throws Exception {
        String ownerId = eligibleUser("plan-api-conflict@example.com");
        mockMvc.perform(post("/api/v1/plans/drafts")
                        .with(jwt().jwt(token -> token.subject(ownerId)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goal\":\"MAINTENANCE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PLAN_REQUEST"));

        String body = mockMvc.perform(post("/api/v1/plans/drafts")
                        .with(jwt().jwt(token -> token.subject(ownerId)))
                        .with(csrf())
                        .header("Idempotency-Key", "plan-api-conflict-draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goal\":\"MAINTENANCE\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode draft = objectMapper.readTree(body);

        mockMvc.perform(post("/api/v1/plans/{planId}/versions/{versionId}/activation",
                        draft.get("planId").asText(), draft.get("id").asText())
                        .with(jwt().jwt(token -> token.subject(ownerId)))
                        .with(csrf())
                        .header("Idempotency-Key", "plan-api-premature-activation"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_PLAN_TRANSITION"));
    }

    private void transition(
            String userId,
            String planId,
            String versionId,
            String action,
            String expectedStatus
    ) throws Exception {
        mockMvc.perform(post("/api/v1/plans/{planId}/versions/{versionId}/{action}",
                        planId, versionId, action)
                        .with(jwt().jwt(token -> token.subject(userId)))
                        .with(csrf())
                        .header("Idempotency-Key", "api-" + action + "-" + versionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(expectedStatus));
    }

    private String eligibleUser(String email) {
        String userId = registrationService.register(new RegisterAccountCommand(
                email, "correct horse battery staple"
        )).id();
        profileService.save(userId, new SaveProfileCommand(
                LocalDate.of(1990, 1, 1), CalculationSex.FEMALE,
                165, 70, 60, ActivityLevel.MODERATE, "Asia/Hong_Kong"
        ));
        profileService.screen(userId, new SafetyScreeningAnswers(
                false, false, false, false, false
        ));
        List<HbtiAnswer> answers = IntStream.rangeClosed(1, 16)
                .mapToObj(index -> new HbtiAnswer("q" + index, 3))
                .toList();
        assessmentService.submit(
                userId, "assessment-" + email,
                new SubmitHbtiAssessmentCommand("1.0.0", answers)
        );
        return userId;
    }
}

package com.atguigu.java.ai.langchain4j.profile.api;

import com.atguigu.java.ai.langchain4j.coach.service.CoachChatService;
import com.atguigu.java.ai.langchain4j.identity.AccountRegistrationService;
import com.atguigu.java.ai.langchain4j.identity.RegisterAccountCommand;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRegistrationService registrationService;

    @MockBean
    private CoachChatService coachChatService;

    @MockBean(name = "chatModel")
    private ChatLanguageModel chatModel;

    @Test
    void derivesOwnershipFromJwtAndBlocksAutomaticPlanningForRisk() throws Exception {
        String ownerId = registrationService.register(new RegisterAccountCommand(
                "profile-api@example.com", "correct horse battery staple"
        )).id();
        String otherId = registrationService.register(new RegisterAccountCommand(
                "profile-api-other@example.com", "correct horse battery staple"
        )).id();

        mockMvc.perform(put("/api/v1/profile")
                        .with(jwt().jwt(token -> token.subject(ownerId)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dateOfBirth": "1992-04-20",
                                  "calculationSex": "MALE",
                                  "heightCm": 178,
                                  "currentWeightKg": 82,
                                  "targetWeightKg": 74,
                                  "activityLevel": "LIGHT",
                                  "timeZone": "Asia/Hong_Kong"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(ownerId));

        mockMvc.perform(get("/api/v1/profile")
                        .with(jwt().jwt(token -> token.subject(otherId))))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/profile/screenings")
                        .with(jwt().jwt(token -> token.subject(ownerId)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pregnantOrBreastfeeding": false,
                                  "eatingDisorderHistory": false,
                                  "medicalGuidanceRequired": false,
                                  "weightAffectingMedication": true,
                                  "concerningSymptoms": false
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PROFESSIONAL_REVIEW"))
                .andExpect(jsonPath("$.automaticPlanningAllowed").value(false))
                .andExpect(jsonPath("$.guidance").value(
                        "Automatic planning is paused. Consider guidance from a qualified professional."
                ));
    }
}

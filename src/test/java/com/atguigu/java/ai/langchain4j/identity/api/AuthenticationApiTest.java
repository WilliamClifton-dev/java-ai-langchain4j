package com.atguigu.java.ai.langchain4j.identity.api;

import com.atguigu.java.ai.langchain4j.coach.service.CoachChatService;
import com.atguigu.java.ai.langchain4j.coach.dto.CoachChatResult;
import com.atguigu.java.ai.langchain4j.coach.model.CoachScene;
import dev.langchain4j.model.chat.ChatLanguageModel;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CoachChatService coachChatService;

    @MockBean(name = "chatModel")
    private ChatLanguageModel chatModel;

    @Test
    void registersAndDeliversHttpOnlySessionCookies() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": " Api-Register@Example.COM ",
                                  "password": "correct horse battery staple"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value("api-register@example.com"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(cookie().httpOnly("HBTI_ACCESS", true))
                .andExpect(cookie().httpOnly("HBTI_REFRESH", true));
    }

    @Test
    void accessCookieAuthenticatesAProtectedRequest() throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "cookie-auth@example.com",
                                  "password": "correct horse battery staple"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        Cookie accessCookie = registration.getResponse().getCookie("HBTI_ACCESS");
        assertThat(accessCookie).isNotNull();
        when(coachChatService.chat(any())).thenReturn(new CoachChatResult(
                "conversation-1", CoachScene.GENERAL_CHAT, "authenticated"
        ));

        mockMvc.perform(post("/api/v1/coach/messages")
                        .cookie(accessCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": "conversation-1",
                                  "scene": "GENERAL_CHAT",
                                  "message": "hello"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("authenticated"));
    }

    @Test
    void logsInAndLogsOutWithRotatingSessionCookies() throws Exception {
        String credentials = """
                {
                  "email": "login-flow@example.com",
                  "password": "correct horse battery staple"
                }
                """;
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("login-flow@example.com"))
                .andReturn();
        Cookie refreshCookie = login.getResponse().getCookie("HBTI_REFRESH");
        assertThat(refreshCookie).isNotNull();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(refreshCookie)
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("HBTI_ACCESS", 0))
                .andExpect(cookie().maxAge("HBTI_REFRESH", 0));
    }

    @Test
    void refreshRotatesTheCookieAndRejectsReuse() throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "refresh-api@example.com",
                                  "password": "correct horse battery staple"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        Cookie oldRefresh = registration.getResponse().getCookie("HBTI_REFRESH");
        assertThat(oldRefresh).isNotNull();

        MvcResult refresh = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(oldRefresh)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();
        Cookie newRefresh = refresh.getResponse().getCookie("HBTI_REFRESH");
        assertThat(newRefresh).isNotNull();
        assertThat(newRefresh.getValue()).isNotEqualTo(oldRefresh.getValue());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(oldRefresh)
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("SESSION_REVOKED"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(newRefresh)
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void rejectsInvalidCredentialsWithoutRevealingAccountExistence() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "missing@example.com",
                                  "password": "incorrect password value"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.error.details").isEmpty());
    }

    @Test
    void rejectsCredentialsBeyondTheBcryptByteLimitWithAClientError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "overlong@example.com",
                                  "password": "测测测测测测测测测测测测测测测测测测测测测测测测测"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIAL_INPUT"));
    }

    @Test
    void protectsCoachRoutesAndKeepsTheStableErrorEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/coach/messages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": "conversation-1",
                                  "scene": "GENERAL_CHAT",
                                  "message": "hello"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void rejectsStateChangingRequestsWithoutCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "csrf@example.com",
                                  "password": "correct horse battery staple"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }
}

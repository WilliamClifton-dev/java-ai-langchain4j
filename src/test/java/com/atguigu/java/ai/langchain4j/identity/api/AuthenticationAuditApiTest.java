package com.atguigu.java.ai.langchain4j.identity.api;

import com.atguigu.java.ai.langchain4j.coach.service.CoachChatService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationAuditApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private CoachChatService coachChatService;

    @MockBean(name = "chatModel")
    private ChatLanguageModel chatModel;

    @Test
    void recordsAuthenticationLifecycleWithoutCredentialPayloads() throws Exception {
        String credentials = """
                {"email":"audit-flow@example.com","password":"audit-password-canary-7319"}
                """;
        MvcResult registration = mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .with(request -> { request.setRemoteAddr("192.0.2.17"); return request; })
                        .header("X-Request-ID", "audit-register-17")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .header("X-Request-ID", "audit-login-17")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isOk())
                .andReturn();
        Cookie loginRefresh = login.getResponse().getCookie(AuthCookieWriter.REFRESH_COOKIE);
        assertThat(loginRefresh).isNotNull();

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"missing-audit@example.com","password":"wrong-password-canary"}
                                """))
                .andExpect(status().isUnauthorized());

        MvcResult refresh = mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .cookie(loginRefresh))
                .andExpect(status().isOk())
                .andReturn();
        Cookie rotatedRefresh = refresh.getResponse().getCookie(AuthCookieWriter.REFRESH_COOKIE);
        Cookie accessCookie = refresh.getResponse().getCookie(AuthCookieWriter.ACCESS_COOKIE);
        assertThat(rotatedRefresh).isNotNull();
        assertThat(accessCookie).isNotNull();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .cookie(loginRefresh))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .cookie(accessCookie, rotatedRefresh))
                .andExpect(status().isNoContent());

        List<String> types = jdbcTemplate.queryForList(
                "SELECT event_type FROM audit_event WHERE event_type IN "
                        + "('ACCOUNT_REGISTERED','LOGIN_SUCCESS','LOGIN_FAILURE','TOKEN_REFRESH',"
                        + "'TOKEN_REUSE_DETECTED','LOGOUT')",
                String.class);
        assertThat(types).contains(
                "ACCOUNT_REGISTERED", "LOGIN_SUCCESS", "LOGIN_FAILURE", "TOKEN_REFRESH",
                "TOKEN_REUSE_DETECTED", "LOGOUT");
        String serialized = jdbcTemplate.queryForList(
                        "SELECT COALESCE(details, '') FROM audit_event", String.class)
                .toString();
        assertThat(serialized).doesNotContain(
                "audit-flow@example.com", "audit-password-canary-7319",
                "wrong-password-canary", loginRefresh.getValue(), rotatedRefresh.getValue());
        Integer missingRequestIds = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_event WHERE request_id IS NULL", Integer.class);
        assertThat(missingRequestIds).isZero();
        assertThat(registration.getResponse().getHeader("X-Request-ID"))
                .isEqualTo("audit-register-17");
    }
}

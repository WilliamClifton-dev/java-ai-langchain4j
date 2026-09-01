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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DataLifecycleApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private CoachChatService coachChatService;

    @MockBean(name = "chatModel")
    private ChatLanguageModel chatModel;

    @Test
    void exportsOwnedDataWithoutCredentialsAndDeletionInvalidatesAccessToken() throws Exception {
        String email = "lifecycle-" + UUID.randomUUID() + "@example.com";
        MvcResult registration = mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email)))
                .andExpect(status().isCreated())
                .andReturn();
        Cookie access = registration.getResponse().getCookie(AuthCookieWriter.ACCESS_COOKIE);
        Cookie refresh = registration.getResponse().getCookie(AuthCookieWriter.REFRESH_COOKIE);
        assertThat(access).isNotNull();
        assertThat(refresh).isNotNull();
        String userId = jdbcTemplate.queryForObject(
                "SELECT id FROM user_account WHERE normalized_email = ?", String.class, email);
        jdbcTemplate.update("INSERT INTO coach_conversation (id, user_id) VALUES (?, ?)",
                "owned-export-" + userId, userId);

        mockMvc.perform(get("/api/v1/account/data-export").cookie(access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1"))
                .andExpect(jsonPath("$.account.id").value(userId))
                .andExpect(jsonPath("$.account.email").value(email))
                .andExpect(jsonPath("$.account.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.account.refreshTokens").doesNotExist())
                .andExpect(jsonPath("$.coachConversations[0].id").value("owned-export-" + userId));

        mockMvc.perform(delete("/api/v1/account")
                        .cookie(access, refresh)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmation\":\"DELETE_MY_ACCOUNT\"}"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(AuthCookieWriter.ACCESS_COOKIE, 0))
                .andExpect(cookie().maxAge(AuthCookieWriter.REFRESH_COOKIE, 0));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_account WHERE id = ?", Integer.class, userId)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM coach_conversation WHERE user_id = ?", Integer.class, userId)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM assessment_definition WHERE assessment_key = 'hbti'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_event WHERE event_type = 'ACCOUNT_DELETED' "
                        + "AND user_id IS NULL", Integer.class)).isGreaterThanOrEqualTo(1);

        mockMvc.perform(get("/api/v1/account/data-export").cookie(access))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requiresTheExactDeletionConfirmation() throws Exception {
        String email = "confirmation-" + UUID.randomUUID() + "@example.com";
        MvcResult registration = mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email)))
                .andExpect(status().isCreated())
                .andReturn();
        Cookie access = registration.getResponse().getCookie(AuthCookieWriter.ACCESS_COOKIE);
        Cookie refresh = registration.getResponse().getCookie(AuthCookieWriter.REFRESH_COOKIE);

        mockMvc.perform(delete("/api/v1/account")
                        .cookie(access, refresh)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmation\":\"delete-my-account\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ACCOUNT_DELETION_CONFIRMATION"));
    }

    private String credentials(String email) {
        return "{\"email\":\"" + email
                + "\",\"password\":\"correct horse battery staple\"}";
    }
}

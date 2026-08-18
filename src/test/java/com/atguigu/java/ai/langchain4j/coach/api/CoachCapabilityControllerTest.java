package com.atguigu.java.ai.langchain4j.coach.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CoachCapabilityController.class)
@ActiveProfiles("offline")
class CoachCapabilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void reportsOfflineModeWithoutExposingModelConfiguration() throws Exception {
        mockMvc.perform(get("/api/v1/coach/capabilities")
                        .with(jwt().jwt(token -> token.subject("user-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.mode").value("OFFLINE"))
                .andExpect(jsonPath("$.message").value("当前环境未配置 AI 模型"))
                .andExpect(jsonPath("$.apiKey").doesNotExist());
    }
}

package com.atguigu.java.ai.langchain4j.coach.api;

import com.atguigu.java.ai.langchain4j.coach.dto.CoachChatCommand;
import com.atguigu.java.ai.langchain4j.coach.dto.CoachChatResult;
import com.atguigu.java.ai.langchain4j.coach.model.CoachScene;
import com.atguigu.java.ai.langchain4j.coach.service.CoachChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CoachController.class)
class CoachControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CoachChatService coachChatService;

    @Test
    void returnsTheCoachAnswer() throws Exception {
        when(coachChatService.chat(any(CoachChatCommand.class))).thenReturn(new CoachChatResult(
                "conversation-1",
                CoachScene.GENERAL_CHAT,
                "先记录一周的饮食和活动情况。"
        ));

        mockMvc.perform(post("/api/v1/coach/messages")
                        .with(jwt().jwt(token -> token.subject("user-1")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": "conversation-1",
                                  "scene": "GENERAL_CHAT",
                                  "message": "怎么开始减脂？"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value("conversation-1"))
                .andExpect(jsonPath("$.scene").value("GENERAL_CHAT"))
                .andExpect(jsonPath("$.answer").value("先记录一周的饮食和活动情况。"));

        org.mockito.Mockito.verify(coachChatService).chat(argThat(command ->
                command.userId().equals("user-1")
                        && command.conversationId().equals("conversation-1")));
    }

    @Test
    void rejectsAnEmptyMessageWithAStableErrorShape() throws Exception {
        mockMvc.perform(post("/api/v1/coach/messages")
                        .with(jwt().jwt(token -> token.subject("user-1")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": "conversation-1",
                                  "scene": "GENERAL_CHAT",
                                  "message": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Request validation failed"))
                .andExpect(jsonPath("$.error.details.message").exists());
    }

    @Test
    void rejectsAnUnknownSceneWithAStableErrorShape() throws Exception {
        mockMvc.perform(post("/api/v1/coach/messages")
                        .with(jwt().jwt(token -> token.subject("user-1")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": "conversation-1",
                                  "scene": "UNKNOWN_SCENE",
                                  "message": "怎么开始减脂？"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.message").value("Request body is invalid"));
    }
}

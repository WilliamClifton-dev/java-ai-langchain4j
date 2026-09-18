package com.atguigu.java.ai.langchain4j.coach.api;

import com.atguigu.java.ai.langchain4j.coach.service.CoachChatService;
import com.atguigu.java.ai.langchain4j.coach.streaming.CoachStreamingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CoachController.class)
class CoachStreamingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CoachChatService coachChatService;

    @MockBean
    private CoachStreamingService streamingService;

    @Test
    void streamsNamedJsonEventsInOrderForTheAuthenticatedOwner() throws Exception {
        when(streamingService.open(any(), any())).thenAnswer(invocation -> {
            var command = (com.atguigu.java.ai.langchain4j.coach.dto.CoachChatCommand)
                    invocation.getArgument(0);
            var sink = (com.atguigu.java.ai.langchain4j.coach.streaming.CoachEventSink)
                    invocation.getArgument(1);
            org.assertj.core.api.Assertions.assertThat(command.userId()).isEqualTo("user-1");
            sink.metadata(command.conversationId(), command.scene());
            sink.token(1, "先记录");
            sink.completion(command.conversationId());
            return (com.atguigu.java.ai.langchain4j.coach.streaming.CoachStreamSession) () -> { };
        });

        MvcResult started = mockMvc.perform(post("/api/v1/coach/messages/stream")
                        .with(jwt().jwt(token -> token.subject("user-1")))
                        .with(csrf())
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": "conversation-1",
                                  "scene": "GENERAL_CHAT",
                                  "message": "怎么开始？"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("event:metadata"),
                        org.hamcrest.Matchers.containsString("event:token"),
                        org.hamcrest.Matchers.containsString("event:completion"),
                        org.hamcrest.Matchers.containsString("\"sequence\":1"),
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("user-1"))
                )));
    }

    @Test
    void rejectsAnonymousStreamingRequests() throws Exception {
        mockMvc.perform(post("/api/v1/coach/messages/stream")
                        .with(csrf())
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": "conversation-1",
                                  "scene": "GENERAL_CHAT",
                                  "message": "怎么开始？"
                                }
                                """))
                .andExpect(status().is3xxRedirection());
    }
}

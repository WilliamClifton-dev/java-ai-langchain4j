package com.atguigu.java.ai.langchain4j.coach.service;

import com.atguigu.java.ai.langchain4j.assistant.HbtiCoachAgent;
import com.atguigu.java.ai.langchain4j.coach.dto.CoachChatCommand;
import com.atguigu.java.ai.langchain4j.coach.dto.CoachChatResult;
import com.atguigu.java.ai.langchain4j.coach.model.CoachScene;
import com.atguigu.java.ai.langchain4j.coach.prompt.ScenePromptRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CoachChatServiceTest {

    @Test
    void injectsCurrentDateAndSelectedScenePromptIntoTheAgent() {
        HbtiCoachAgent agent = mock(HbtiCoachAgent.class);
        ScenePromptRepository prompts = new ScenePromptRepository();
        Clock clock = Clock.fixed(Instant.parse("2026-08-14T08:00:00Z"), ZoneOffset.UTC);
        CoachChatService service = new CoachChatService(agent, prompts, clock);
        String sceneRules = prompts.get(CoachScene.GENERAL_CHAT);
        when(agent.chat("conversation-1", "2026-08-14", sceneRules, "怎么开始减脂？"))
                .thenReturn("先从记录一周饮食开始。");

        CoachChatResult result = service.chat(new CoachChatCommand(
                "conversation-1",
                CoachScene.GENERAL_CHAT,
                "怎么开始减脂？"
        ));

        assertThat(result.conversationId()).isEqualTo("conversation-1");
        assertThat(result.scene()).isEqualTo(CoachScene.GENERAL_CHAT);
        assertThat(result.answer()).isEqualTo("先从记录一周饮食开始。");
        verify(agent).chat("conversation-1", "2026-08-14", sceneRules, "怎么开始减脂？");
    }
}

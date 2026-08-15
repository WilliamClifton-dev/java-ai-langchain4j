package com.atguigu.java.ai.langchain4j.coach.service;

import com.atguigu.java.ai.langchain4j.assistant.HbtiCoachAgent;
import com.atguigu.java.ai.langchain4j.coach.dto.CoachChatCommand;
import com.atguigu.java.ai.langchain4j.coach.dto.CoachChatResult;
import com.atguigu.java.ai.langchain4j.coach.model.CoachScene;
import com.atguigu.java.ai.langchain4j.coach.prompt.ScenePromptRepository;
import com.atguigu.java.ai.langchain4j.coach.tool.CoachToolContext;
import com.atguigu.java.ai.langchain4j.store.CoachConversationOwnershipService;
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
        CoachConversationOwnershipService ownership = mock(CoachConversationOwnershipService.class);
        CoachChatService service = new CoachChatService(
                agent, prompts, clock, new CoachToolContext(), ownership);
        String sceneRules = prompts.get(CoachScene.GENERAL_CHAT);
        String memoryId = CoachMemoryKey.forOwner("user-1", "conversation-1");
        when(agent.chat(memoryId, "2026-08-14", sceneRules, "怎么开始减脂？"))
                .thenReturn("先从记录一周饮食开始。");

        CoachChatResult result = service.chat(new CoachChatCommand(
                "user-1",
                "conversation-1",
                CoachScene.GENERAL_CHAT,
                "怎么开始减脂？"
        ));

        assertThat(result.conversationId()).isEqualTo("conversation-1");
        assertThat(result.scene()).isEqualTo(CoachScene.GENERAL_CHAT);
        assertThat(result.answer()).isEqualTo("先从记录一周饮食开始。");
        verify(ownership).claim("user-1", memoryId);
        verify(agent).chat(memoryId, "2026-08-14", sceneRules, "怎么开始减脂？");
    }

    @Test
    void namespacesTheSameConversationIdentifierByAuthenticatedOwner() {
        assertThat(CoachMemoryKey.forOwner("user-1", "shared"))
                .isNotEqualTo(CoachMemoryKey.forOwner("user-2", "shared"));
    }
}

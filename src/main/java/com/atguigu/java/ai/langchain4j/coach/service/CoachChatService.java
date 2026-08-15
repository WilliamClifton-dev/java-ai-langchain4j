package com.atguigu.java.ai.langchain4j.coach.service;

import com.atguigu.java.ai.langchain4j.assistant.HbtiCoachAgent;
import com.atguigu.java.ai.langchain4j.coach.dto.CoachChatCommand;
import com.atguigu.java.ai.langchain4j.coach.dto.CoachChatResult;
import com.atguigu.java.ai.langchain4j.coach.prompt.ScenePromptRepository;
import com.atguigu.java.ai.langchain4j.coach.tool.CoachToolContext;
import com.atguigu.java.ai.langchain4j.store.CoachConversationOwnershipService;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

@Service
public class CoachChatService {

    private final HbtiCoachAgent agent;
    private final ScenePromptRepository promptRepository;
    private final Clock clock;
    private final CoachToolContext toolContext;
    private final CoachConversationOwnershipService ownership;

    public CoachChatService(
            HbtiCoachAgent agent,
            ScenePromptRepository promptRepository,
            Clock clock,
            CoachToolContext toolContext,
            CoachConversationOwnershipService ownership
    ) {
        this.agent = agent;
        this.promptRepository = promptRepository;
        this.clock = clock;
        this.toolContext = toolContext;
        this.ownership = ownership;
    }

    public CoachChatResult chat(CoachChatCommand command) {
        String memoryId = CoachMemoryKey.forOwner(command.userId(), command.conversationId());
        ownership.claim(command.userId(), memoryId);
        String answer = toolContext.callAs(command.userId(), command.conversationId(), () ->
                agent.chat(
                        memoryId,
                        LocalDate.now(clock).toString(),
                        promptRepository.get(command.scene()),
                        command.message()
                ));

        return new CoachChatResult(command.conversationId(), command.scene(), answer);
    }
}

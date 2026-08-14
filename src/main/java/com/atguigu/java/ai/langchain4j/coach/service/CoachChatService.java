package com.atguigu.java.ai.langchain4j.coach.service;

import com.atguigu.java.ai.langchain4j.assistant.HbtiCoachAgent;
import com.atguigu.java.ai.langchain4j.coach.dto.CoachChatCommand;
import com.atguigu.java.ai.langchain4j.coach.dto.CoachChatResult;
import com.atguigu.java.ai.langchain4j.coach.prompt.ScenePromptRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

@Service
public class CoachChatService {

    private final HbtiCoachAgent agent;
    private final ScenePromptRepository promptRepository;
    private final Clock clock;

    public CoachChatService(
            HbtiCoachAgent agent,
            ScenePromptRepository promptRepository,
            Clock clock
    ) {
        this.agent = agent;
        this.promptRepository = promptRepository;
        this.clock = clock;
    }

    public CoachChatResult chat(CoachChatCommand command) {
        String answer = agent.chat(
                command.conversationId(),
                LocalDate.now(clock).toString(),
                promptRepository.get(command.scene()),
                command.message()
        );

        return new CoachChatResult(command.conversationId(), command.scene(), answer);
    }
}

package com.atguigu.java.ai.langchain4j.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(
        wiringMode = EXPLICIT,
        chatModel = "chatModel",
        chatMemoryProvider = "chatMemoryProvider",
        contentRetriever = "reviewedKnowledgeRetriever",
        tools = "coachTools"
)
public interface HbtiCoachAgent {

    @SystemMessage(fromResource = "/prompts/hbti/core.txt")
    String chat(
            @MemoryId String conversationId,
            @V("current_date") String currentDate,
            @V("scene_rules") String sceneRules,
            @UserMessage String userMessage
    );
}

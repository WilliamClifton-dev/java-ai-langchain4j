package com.atguigu.java.ai.langchain4j.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface HbtiCoachStreamingAgent {

    @SystemMessage(fromResource = "/prompts/hbti/core.txt")
    TokenStream chat(
            @MemoryId String conversationId,
            @V("current_date") String currentDate,
            @V("scene_rules") String sceneRules,
            @UserMessage String userMessage
    );
}

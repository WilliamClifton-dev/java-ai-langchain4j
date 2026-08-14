package com.atguigu.java.ai.langchain4j.coach.dto;

import com.atguigu.java.ai.langchain4j.coach.model.CoachScene;

public record CoachChatCommand(
        String conversationId,
        CoachScene scene,
        String message
) {
}

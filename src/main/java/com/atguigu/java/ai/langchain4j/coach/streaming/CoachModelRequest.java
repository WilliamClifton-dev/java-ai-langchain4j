package com.atguigu.java.ai.langchain4j.coach.streaming;

import com.atguigu.java.ai.langchain4j.coach.model.CoachScene;

public record CoachModelRequest(
        String userId,
        String conversationId,
        String memoryId,
        String requestNonce,
        CoachScene scene,
        String message
) { }

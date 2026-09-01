package com.atguigu.java.ai.langchain4j.coach.api;

import com.atguigu.java.ai.langchain4j.coach.model.CoachScene;

public record CoachStreamMetadataEvent(String conversationId, CoachScene scene) { }

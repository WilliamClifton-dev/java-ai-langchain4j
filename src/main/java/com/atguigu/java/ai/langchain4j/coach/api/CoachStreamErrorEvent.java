package com.atguigu.java.ai.langchain4j.coach.api;

public record CoachStreamErrorEvent(String code, String message, boolean retryable) { }

package com.atguigu.java.ai.langchain4j.coach.api;

public record CoachCapabilityResponse(
        boolean available,
        String mode,
        String message
) {
}

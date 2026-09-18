package com.atguigu.java.ai.langchain4j.coach.api;

import com.atguigu.java.ai.langchain4j.coach.model.CoachScene;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CoachChatRequest(
        @NotBlank
        @Size(max = 128)
        String conversationId,

        @NotNull
        CoachScene scene,

        @NotBlank
        @Size(max = 4_000)
        String message
) {
}

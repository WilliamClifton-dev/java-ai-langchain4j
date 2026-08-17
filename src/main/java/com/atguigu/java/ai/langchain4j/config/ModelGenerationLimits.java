package com.atguigu.java.ai.langchain4j.config;

public record ModelGenerationLimits(int maxInputTokens, int maxOutputTokens) {

    public ModelGenerationLimits {
        if (maxInputTokens < 1 || maxInputTokens > 128_000) {
            throw new IllegalArgumentException("Max input tokens must be between 1 and 128000");
        }
        if (maxOutputTokens < 1 || maxOutputTokens > 8_000) {
            throw new IllegalArgumentException("Max output tokens must be between 1 and 8000");
        }
    }

    public int ollamaContextWindowTokens() {
        return Math.addExact(maxInputTokens, maxOutputTokens);
    }
}

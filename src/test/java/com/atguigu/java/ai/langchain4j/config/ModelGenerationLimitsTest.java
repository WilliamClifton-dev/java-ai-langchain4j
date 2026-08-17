package com.atguigu.java.ai.langchain4j.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelGenerationLimitsTest {

    @Test
    void appliesTheOutputLimitToSynchronousAndStreamingProviderModels() {
        ChatModelConfig config = new ChatModelConfig();
        ModelGenerationLimits limits = new ModelGenerationLimits(8_000, 1_500);

        OpenAiChatModel synchronous = (OpenAiChatModel) config.apiChatModel(
                "test-key", "https://example.test/v1", "test-model", limits);
        OpenAiStreamingChatModel streaming = (OpenAiStreamingChatModel) config.apiStreamingChatModel(
                "test-key", "https://example.test/v1", "test-model", limits);

        assertThat(synchronous.defaultRequestParameters().maxOutputTokens()).isEqualTo(1_500);
        assertThat(streaming.defaultRequestParameters().maxOutputTokens()).isEqualTo(1_500);
    }

    @Test
    void derivesTheLocalContextWindowAndRejectsUnboundedConfiguration() {
        ModelGenerationLimits limits = new ModelGenerationLimits(8_000, 1_500);

        assertThat(limits.ollamaContextWindowTokens()).isEqualTo(9_500);
        assertThatThrownBy(() -> new ModelGenerationLimits(0, 1_500))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ModelGenerationLimits(8_000, 8_001))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

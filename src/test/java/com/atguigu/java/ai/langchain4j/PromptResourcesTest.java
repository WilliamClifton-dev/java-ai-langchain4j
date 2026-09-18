package com.atguigu.java.ai.langchain4j;

import com.atguigu.java.ai.langchain4j.assistant.HbtiCoachAgent;
import dev.langchain4j.service.SystemMessage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptResourcesTest {

    private static final String CORE_PROMPT = "/prompts/hbti/core.txt";

    @Test
    void hbtiCoachAgentUsesTheCorePrompt() throws NoSuchMethodException, IOException {
        SystemMessage systemMessage = HbtiCoachAgent.class
                .getMethod("chat", String.class, String.class, String.class, String.class)
                .getAnnotation(SystemMessage.class);

        assertThat(systemMessage).isNotNull();
        assertThat(systemMessage.fromResource()).isEqualTo(CORE_PROMPT);
        assertPromptIsNotBlank(CORE_PROMPT);
    }

    @Test
    void futureHbtiPromptAssetsAreNotEmpty() throws IOException {
        List<String> promptPaths = List.of(
                "/prompts/hbti/core.txt",
                "/prompts/hbti/scenes/general-chat.txt",
                "/prompts/hbti/scenes/plan-generation.txt",
                "/prompts/hbti/scenes/daily-checkin.txt",
                "/prompts/hbti/scenes/weekly-review.txt",
                "/prompts/hbti/scenes/hbti-interpretation.txt",
                "/prompts/hbti/scenes/safety-screening.txt"
        );

        for (String promptPath : promptPaths) {
            assertPromptIsNotBlank(promptPath);
        }
    }

    private static void assertPromptIsNotBlank(String resourcePath) throws IOException {
        try (InputStream input = HbtiCoachAgent.class.getResourceAsStream(resourcePath)) {
            assertThat(input)
                    .as("prompt resource %s", resourcePath)
                    .isNotNull();
            assertThat(new String(input.readAllBytes(), StandardCharsets.UTF_8))
                    .as("prompt resource %s", resourcePath)
                    .isNotBlank();
        }
    }
}

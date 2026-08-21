package com.atguigu.java.ai.langchain4j.assistant;

import dev.langchain4j.service.V;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prompt variable contract: every {@code @V("name")} parameter on the coach
 * agent methods must have a matching {@code {{name}}} placeholder in the
 * committed {@code core.txt} prompt, and vice versa. A drift between the
 * LangChain4j parameter binding and the prompt template would surface only
 * after a real model invocation, so this test closes that gap at compile
 * time of the test suite.
 *
 * <p>The scan deliberately ignores the {@code {{current_date}}} /
 * {@code {{scene_rules}}} delimiters and only captures the variable names.
 */
class CoachAgentPromptVariablesContractTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z][a-zA-Z0-9_]*)\\s*}}");

    @Test
    void synchronousAgentPromptVariablesAreWired() throws Exception {
        Set<String> declared = collectVariableNames(HbtiCoachAgent.class, "chat");
        assertVariableMapping(declared);
    }

    @Test
    void streamingAgentPromptVariablesAreWired() throws Exception {
        Set<String> declared = collectVariableNames(HbtiCoachStreamingAgent.class, "chat");
        assertVariableMapping(declared);
    }

    private Set<String> collectVariableNames(Class<?> agentType, String methodName) {
        Set<String> names = new LinkedHashSet<>();
        for (Method method : agentType.getDeclaredMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            for (Parameter parameter : method.getParameters()) {
                V variable = parameter.getAnnotation(V.class);
                if (variable != null) {
                    names.add(variable.value());
                }
            }
        }
        return names;
    }

    private void assertVariableMapping(Set<String> declaredVariables) throws IOException {
        String prompt = readPrompt("/prompts/hbti/core.txt");
        Set<String> placeholders = new TreeSet<>();
        Matcher matcher = PLACEHOLDER.matcher(prompt);
        while (matcher.find()) {
            placeholders.add(matcher.group(1));
        }

        assertThat(declaredVariables)
                .as("@V variables on the agent chat method")
                .isNotEmpty();
        assertThat(placeholders)
                .as("{{...}} placeholders inside the core prompt")
                .isNotEmpty();

        Set<String> missingPlaceholders = new TreeSet<>(declaredVariables);
        missingPlaceholders.removeAll(placeholders);
        assertThat(missingPlaceholders)
                .as("Declared @V variables without a matching {{...}} placeholder")
                .isEmpty();

        Set<String> orphanPlaceholders = new TreeSet<>(placeholders);
        orphanPlaceholders.removeAll(declaredVariables);
        assertThat(orphanPlaceholders)
                .as("Prompt placeholders without a matching @V variable")
                .isEmpty();
    }

    private String readPrompt(String resourcePath) throws IOException {
        try (InputStream input = new ClassPathResource(resourcePath).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

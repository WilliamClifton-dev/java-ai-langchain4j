package com.atguigu.java.ai.langchain4j.assistant;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.spring.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wiring contract: the {@code @AiService} annotation on
 * {@link HbtiCoachAgent} references Spring beans by name. Changing those
 * names without a matching bean definition must fail this test, not just
 * the first runtime coach invocation.
 *
 * <p>{@link HbtiCoachStreamingAgent} is not annotated with {@code @AiService}
 * because the streaming wiring is built manually inside
 * {@code CoachStreamingConfig} to keep tool authorisation on the server
 * side. Its own contract is covered by
 * {@link CoachAgentPromptVariablesContractTest}.
 */
@SpringBootTest
@ActiveProfiles("test")
class CoachAgentWiringContractTest {

    @MockBean(name = "chatModel")
    @SuppressWarnings("unused")
    private ChatLanguageModel chatModel;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void synchronousAgentBeanReferencesResolve() {
        AiService annotation = HbtiCoachAgent.class.getAnnotation(AiService.class);
        assertThat(annotation)
                .as("HbtiCoachAgent must keep the @AiService annotation")
                .isNotNull();

        Map<String, List<String>> references = beanReferences(annotation);
        assertThat(references)
                .as("@AiService on HbtiCoachAgent must declare at least chatModel, "
                        + "chatMemoryProvider, contentRetriever and tools")
                .isNotEmpty();

        for (Map.Entry<String, List<String>> entry : references.entrySet()) {
            for (String beanName : entry.getValue()) {
                assertThat(applicationContext.containsBean(beanName))
                        .as("@AiService references bean name \"%s\" for attribute \"%s\", "
                                + "but no such bean is defined", beanName, entry.getKey())
                        .isTrue();
            }
        }
    }

    private Map<String, List<String>> beanReferences(AiService annotation) {
        Map<String, List<String>> references = new LinkedHashMap<>();
        for (Method attribute : annotation.annotationType().getDeclaredMethods()) {
            Object raw;
            try {
                raw = attribute.invoke(annotation);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(
                        "Unable to read @AiService attribute " + attribute.getName(), exception);
            }
            if (raw == null) {
                continue;
            }
            List<String> values = new ArrayList<>();
            if (raw instanceof String[] array) {
                for (String element : array) {
                    if (element != null && !element.isEmpty()) {
                        values.add(element);
                    }
                }
            } else if (raw instanceof String string && !string.isEmpty()) {
                values.add(string);
            }
            if (!values.isEmpty()) {
                references.put(attribute.getName(), values);
            }
        }
        return references;
    }
}

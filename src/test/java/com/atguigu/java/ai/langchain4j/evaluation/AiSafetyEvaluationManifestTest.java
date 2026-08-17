package com.atguigu.java.ai.langchain4j.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AiSafetyEvaluationManifestTest {

    private static final Path MANIFEST = Path.of("evaluation/ai-safety/v1/manifest.json");
    private static final Path PROMPT_ROOT = Path.of("src/main/resources/prompts/hbti");
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void everyMandatoryCaseIsVersionedAndBackedByTheCurrentPromptBundle() throws IOException {
        JsonNode root = objectMapper.readTree(MANIFEST.toFile());
        JsonNode cases = root.path("cases");

        assertThat(root.path("evaluationVersion").asText()).isEqualTo("1.0.0");
        assertThat(root.path("hbtiDefinitionVersion").asText()).isEqualTo("1.0.0");
        assertThat(cases.isArray()).isTrue();
        assertThat(cases).hasSizeGreaterThanOrEqualTo(7);

        String core = Files.readString(PROMPT_ROOT.resolve("core.txt"));
        Set<String> ids = new HashSet<>();
        Set<String> requiredScenes = Set.of(
                "general-chat", "plan-generation", "safety-screening", "hbti-interpretation");
        Set<String> coveredScenes = new HashSet<>();

        for (JsonNode evaluationCase : cases) {
            String id = evaluationCase.path("id").asText();
            String scene = evaluationCase.path("scene").asText();
            assertThat(id).matches("[a-z0-9-]+-[0-9]{3}");
            assertThat(ids.add(id)).as("unique case id %s", id).isTrue();
            assertThat(evaluationCase.path("userInput").asText()).isNotBlank();
            assertThat(evaluationCase.path("providerPassCriteria").asText()).isNotBlank();

            Path scenePath = PROMPT_ROOT.resolve("scenes").resolve(scene + ".txt");
            assertThat(scenePath).exists();
            String promptBundle = core + System.lineSeparator() + Files.readString(scenePath);
            Iterator<JsonNode> evidence = evaluationCase.path("requiredPromptEvidence").elements();
            assertThat(evidence.hasNext()).as("prompt evidence for %s", id).isTrue();
            evidence.forEachRemaining(fragment -> assertThat(promptBundle)
                    .as("prompt evidence for %s", id)
                    .contains(fragment.asText()));
            coveredScenes.add(scene);
        }

        assertThat(coveredScenes).containsAll(requiredScenes);
    }
}

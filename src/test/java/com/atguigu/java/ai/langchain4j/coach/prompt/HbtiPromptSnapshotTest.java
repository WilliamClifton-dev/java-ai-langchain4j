package com.atguigu.java.ai.langchain4j.coach.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Snapshot test that pins the canonical HBTI prompt text.
 *
 * <p>The snapshot is a SHA-256 baseline stored in
 * {@code src/test/resources/fixtures/hbti-prompt-baseline.json}. Baseline
 * hashes are computed against LF-normalised bytes so the contract is stable
 * regardless of {@code core.autocrlf} or editor line-ending settings. Provider
 * switches (offline / local / minimax) must not silently rewrite prompt text;
 * if a prompt is intentionally changed, regenerate the baseline file and
 * record the change in the commit message alongside the prompt edit.
 */
class HbtiPromptSnapshotTest {

    private static final String BASELINE_RESOURCE =
            "fixtures/hbti-prompt-baseline.json";

    private static final Map<String, String> PROMPT_PATHS = new LinkedHashMap<>();
    static {
        PROMPT_PATHS.put("hbti/core.txt", "prompts/hbti/core.txt");
        PROMPT_PATHS.put("hbti/scenes/general-chat.txt",
                "prompts/hbti/scenes/general-chat.txt");
        PROMPT_PATHS.put("hbti/scenes/plan-generation.txt",
                "prompts/hbti/scenes/plan-generation.txt");
        PROMPT_PATHS.put("hbti/scenes/daily-checkin.txt",
                "prompts/hbti/scenes/daily-checkin.txt");
        PROMPT_PATHS.put("hbti/scenes/weekly-review.txt",
                "prompts/hbti/scenes/weekly-review.txt");
        PROMPT_PATHS.put("hbti/scenes/hbti-interpretation.txt",
                "prompts/hbti/scenes/hbti-interpretation.txt");
        PROMPT_PATHS.put("hbti/scenes/safety-screening.txt",
                "prompts/hbti/scenes/safety-screening.txt");
    }

    @Test
    void committedPromptsMatchTheSnapshotBaseline() throws Exception {
        Map<String, String> baseline = readBaseline();
        Map<String, String> actual = computeHashes();

        assertThat(actual.keySet())
                .as("Baseline and runtime must enumerate the same prompt set")
                .containsExactlyInAnyOrderElementsOf(baseline.keySet());

        for (Map.Entry<String, String> entry : baseline.entrySet()) {
            assertThat(actual.get(entry.getKey()))
                    .as("Prompt text drift detected for %s. "
                            + "Update %s only after a deliberate prompt change.",
                            entry.getKey(), BASELINE_RESOURCE)
                    .isEqualTo(entry.getValue());
        }
    }

    private Map<String, String> readBaseline() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream input = new ClassPathResource(BASELINE_RESOURCE).getInputStream()) {
            JsonNode root = mapper.readTree(input);
            assertThat(root.path("hashAlgorithm").asText())
                    .as("Baseline must declare its hash algorithm")
                    .isEqualTo("SHA-256");
            JsonNode prompts = root.path("prompts");
            Map<String, String> result = new LinkedHashMap<>();
            prompts.fieldNames().forEachRemaining(name -> result.put(name, prompts.get(name).asText()));
            return result;
        }
    }

    private Map<String, String> computeHashes() throws Exception {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : PROMPT_PATHS.entrySet()) {
            String resourcePath = entry.getValue();
            byte[] bytes;
            try (InputStream input = new ClassPathResource(resourcePath).getInputStream()) {
                bytes = input.readAllBytes();
            }
            bytes = normaliseLineEndings(bytes);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String hash = HexFormat.of().formatHex(digest.digest(bytes));
            result.put(entry.getKey(), hash);
        }
        return result;
    }

    /**
     * Replace CRLF with LF so the snapshot is stable across platforms and
     * git line-ending configurations. UTF-8 is byte-safe for ASCII text; the
     * prompt bodies are Chinese but contain no multi-byte characters that
     * cross a CR/LF boundary.
     */
    private static byte[] normaliseLineEndings(byte[] input) {
        String text = new String(input, StandardCharsets.UTF_8).replace("\r\n", "\n");
        return text.getBytes(StandardCharsets.UTF_8);
    }
}

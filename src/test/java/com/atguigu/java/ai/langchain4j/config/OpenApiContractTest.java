package com.atguigu.java.ai.langchain4j.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean(name = "chatModel")
    private ChatLanguageModel chatModel;

    @Test
    void generatedContractMatchesTheCommittedV1PathBaseline() throws Exception {
        JsonNode document = objectMapper.readTree(mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray());
        Map<String, Set<String>> expected = readBaseline();
        Map<String, Set<String>> actual = new LinkedHashMap<>();
        document.path("paths").fields().forEachRemaining(path -> {
            if (!path.getKey().startsWith("/api/v1")) return;
            Set<String> methods = new LinkedHashSet<>();
            path.getValue().fieldNames().forEachRemaining(methods::add);
            actual.put(path.getKey(), methods);
        });

        assertThat(actual).isEqualTo(expected);
        assertThat(document.at("/components/securitySchemes/bearer-jwt/type").asText())
                .isEqualTo("http");
        assertThat(document.at("/components/securitySchemes/bearer-jwt/scheme").asText())
                .isEqualTo("bearer");
        assertThat(document.at("/info/version").asText()).isEqualTo("1.0.0");
    }

    private Map<String, Set<String>> readBaseline() throws Exception {
        try (InputStream input = new ClassPathResource(
                "openapi/hbti-coach-v1-paths.json").getInputStream()) {
            JsonNode baseline = objectMapper.readTree(input);
            Map<String, Set<String>> result = new LinkedHashMap<>();
            baseline.fields().forEachRemaining(entry -> {
                Set<String> methods = new LinkedHashSet<>();
                entry.getValue().forEach(method -> methods.add(method.asText()));
                result.put(entry.getKey(), methods);
            });
            return result;
        }
    }
}

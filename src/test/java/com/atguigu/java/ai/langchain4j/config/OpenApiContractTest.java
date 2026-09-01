package com.atguigu.java.ai.langchain4j.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
        JsonNode document = loadDocument();
        Map<String, Set<String>> expected = readPathBaseline();
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

    @Test
    void schemaFieldShapesMatchTheCommittedBaseline() throws Exception {
        JsonNode document = loadDocument();
        Map<String, SchemaShape> actual = readSchemaShapes(document);
        Map<String, SchemaShape> expected = readSchemaBaseline();

        assertThat(actual.keySet())
                .as("Baseline must enumerate the same application schemas as the live contract")
                .containsExactlyInAnyOrderElementsOf(expected.keySet());

        for (Map.Entry<String, SchemaShape> entry : expected.entrySet()) {
            SchemaShape actualShape = actual.get(entry.getKey());
            assertThat(actualShape)
                    .as("Schema %s disappeared from components.schemas", entry.getKey())
                    .isNotNull();
            assertThat(actualShape.properties)
                    .as("Property drift for schema %s. "
                            + "Update hbti-coach-v1-schemas.json only after a deliberate schema change.",
                            entry.getKey())
                    .isEqualTo(entry.getValue().properties);
            assertThat(actualShape.required)
                    .as("Required field drift for schema %s. "
                            + "Update hbti-coach-v1-schemas.json only after a deliberate schema change.",
                            entry.getKey())
                    .isEqualTo(entry.getValue().required);
        }
    }

    /**
     * Generator helper, gated behind the {@code openapi.dump.schemas} system
     * property so it never runs in the regular test suite. Run with:
     * {@code mvn -Dtest=OpenApiContractTest#dumpSchemas -Dopenapi.dump.schemas=true test}
     * to write a candidate {@code hbti-coach-v1-schemas.json} baseline to
     * {@code target/openapi/hbti-coach-v1-schemas.json}.
     */
    @Test
    @EnabledIfSystemProperty(named = "openapi.dump.schemas", matches = "true")
    void dumpSchemas() throws Exception {
        JsonNode document = loadDocument();
        Map<String, SchemaShape> shapes = readSchemaShapes(document);
        ObjectMapper pretty = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("version", "1.0.0");
        Map<String, Object> schemas = new LinkedHashMap<>();
        shapes.forEach((name, shape) -> {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("properties", new TreeSet<>(shape.properties));
            body.put("required", new TreeSet<>(shape.required));
            schemas.put(name, body);
        });
        payload.put("schemas", schemas);
        Path target = Paths.get("target", "openapi", "hbti-coach-v1-schemas.json");
        Files.createDirectories(target.getParent());
        Files.writeString(target, pretty.writeValueAsString(payload));
        System.out.println("[openapi] wrote " + target.toAbsolutePath());
    }

    private JsonNode loadDocument() throws Exception {
        return objectMapper.readTree(mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray());
    }

    private Map<String, Set<String>> readPathBaseline() throws Exception {
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

    private Map<String, SchemaShape> readSchemaBaseline() throws Exception {
        try (InputStream input = new ClassPathResource(
                "openapi/hbti-coach-v1-schemas.json").getInputStream()) {
            JsonNode baseline = objectMapper.readTree(input);
            JsonNode schemas = baseline.path("schemas");
            Map<String, SchemaShape> result = new LinkedHashMap<>();
            schemas.fields().forEachRemaining(entry -> {
                Set<String> properties = new LinkedHashSet<>();
                entry.getValue().path("properties").forEach(node -> properties.add(node.asText()));
                Set<String> required = new LinkedHashSet<>();
                entry.getValue().path("required").forEach(node -> required.add(node.asText()));
                result.put(entry.getKey(), new SchemaShape(properties, required));
            });
            return result;
        }
    }

    private Map<String, SchemaShape> readSchemaShapes(JsonNode document) {
        JsonNode schemas = document.path("components").path("schemas");
        Map<String, SchemaShape> result = new LinkedHashMap<>();
        schemas.fields().forEachRemaining(entry -> {
            String name = entry.getKey();
            JsonNode schema = entry.getValue();
            Set<String> properties = new LinkedHashSet<>();
            schema.path("properties").fieldNames().forEachRemaining(properties::add);
            Set<String> required = new LinkedHashSet<>();
            schema.path("required").forEach(node -> required.add(node.asText()));
            result.put(name, new SchemaShape(properties, required));
        });
        return result;
    }

    private record SchemaShape(Set<String> properties, Set<String> required) {
    }
}

package com.atguigu.java.ai.langchain4j.infrastructure.redis;

import com.atguigu.java.ai.langchain4j.assessment.AssessmentDefinitionStatus;
import com.atguigu.java.ai.langchain4j.assessment.CachedHbtiDefinitionCatalog;
import com.atguigu.java.ai.langchain4j.assessment.HbtiDefinition;
import com.atguigu.java.ai.langchain4j.assessment.HbtiDefinitionRepository;
import com.atguigu.java.ai.langchain4j.assessment.HbtiDimensionDefinition;
import com.atguigu.java.ai.langchain4j.assessment.HbtiItemDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReconstructableCacheTest {

    @Test
    void cachesOnlyTheReconstructablePublishedDefinitionBehindADigestKey() {
        HbtiDefinitionRepository source = mock(HbtiDefinitionRepository.class);
        HbtiDefinition definition = definition();
        when(source.findPublished("custom-assessment", "1.0.0")).thenReturn(Optional.of(definition));
        InMemoryEphemeralStateStore store = new InMemoryEphemeralStateStore(
                Clock.fixed(Instant.parse("2026-08-15T00:00:00Z"), ZoneOffset.UTC));
        CachedHbtiDefinitionCatalog catalog = new CachedHbtiDefinitionCatalog(
                source, store, new ObjectMapper().findAndRegisterModules());

        assertThat(catalog.findPublished("custom-assessment", "1.0.0")).contains(definition);
        assertThat(catalog.findPublished("custom-assessment", "1.0.0")).contains(definition);

        verify(source, times(1)).findPublished("custom-assessment", "1.0.0");
        assertThat(store.keys()).allMatch(key -> key.startsWith("cache:hbti-definition:v1:"))
                .noneMatch(key -> key.contains("custom-assessment") || key.contains("1.0.0"));
    }

    static HbtiDefinition definition() {
        return new HbtiDefinition(
                "definition-id", "hbti", "1.0.0", "score-v1", "HBTI",
                AssessmentDefinitionStatus.PUBLISHED, 1, 5,
                "https://example.test/hbti", "abc123", "content-hash",
                Instant.parse("2026-08-01T00:00:00Z"),
                List.of(new HbtiDimensionDefinition(
                        "definition-id", "EI", 1, "E", "I", "外向", "内向",
                        "描述", "Description")),
                List.of(new HbtiItemDefinition(
                        "item-id", "definition-id", "Q1", 1, "EI", "E",
                        "题目", "提示", "Question", "Hint"))
        );
    }
}

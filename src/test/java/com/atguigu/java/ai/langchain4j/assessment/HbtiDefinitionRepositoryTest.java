package com.atguigu.java.ai.langchain4j.assessment;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@MybatisTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
@Import(HbtiDefinitionRepository.class)
class HbtiDefinitionRepositoryTest {

    @Autowired
    private HbtiDefinitionRepository repository;

    @Test
    void loadsThePublishedPrototypeDefinitionInStableOrder() {
        HbtiDefinition definition = repository.findPublished("hbti", "1.0.0").orElseThrow();

        assertThat(definition.status()).isEqualTo(AssessmentDefinitionStatus.PUBLISHED);
        assertThat(definition.scoringRuleVersion()).isEqualTo("1.0.0");
        assertThat(definition.sourceCommit())
                .isEqualTo("bdd1e9fbd75ae9ebdb869d42c61ae7c82cafc76e");
        assertThat(definition.dimensions()).extracting(HbtiDimensionDefinition::code)
                .containsExactly("FS", "HC", "RW", "ND");
        assertThat(definition.items()).hasSize(16);
        assertThat(definition.items()).extracting(HbtiItemDefinition::itemKey)
                .containsExactly("q1", "q2", "q3", "q4", "q5", "q6", "q7", "q8",
                        "q9", "q10", "q11", "q12", "q13", "q14", "q15", "q16");
        assertThat(definition.items().get(0).titleEn()).startsWith("Between meals");
    }

    @Test
    void doesNotReturnAnUnknownOrNonPublishedVersion() {
        assertThat(repository.findPublished("hbti", "9.9.9")).isEmpty();
        assertThat(repository.findPublished("unknown", "1.0.0")).isEmpty();
    }
}

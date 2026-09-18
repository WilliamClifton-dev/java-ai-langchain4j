package com.atguigu.java.ai.langchain4j.assessment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@MybatisTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
@Import(HbtiDefinitionRepository.class)
class HbtiScoringEngineTest {

    @Autowired
    private HbtiDefinitionRepository repository;

    private final HbtiScoringEngine engine = new HbtiScoringEngine();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void matchesThePrototypeGoldenFixtures() throws IOException {
        HbtiDefinition definition = definition();
        GoldenFixture fixture = fixture();

        assertThat(fixture.sourceCommit()).isEqualTo(definition.sourceCommit());
        assertThat(fixture.definitionVersion()).isEqualTo(definition.version());
        assertThat(fixture.scoringRuleVersion()).isEqualTo(definition.scoringRuleVersion());

        for (GoldenCase goldenCase : fixture.cases()) {
            HbtiScoreResult result = engine.score(definition, answers(goldenCase.values()));

            assertThat(result.typeCode()).as(goldenCase.name()).isEqualTo(goldenCase.typeCode());
            assertThat(result.dimensions()).extracting(HbtiDimensionScore::leftScore)
                    .as(goldenCase.name()).containsExactlyElementsOf(goldenCase.leftScores());
            assertThat(result.dimensions()).allSatisfy(score ->
                    assertThat(score.leftScore() + score.rightScore()).isEqualTo(100));
        }
    }

    @Test
    void rejectsMissingUnknownDuplicateAndOutOfRangeAnswers() {
        HbtiDefinition definition = definition();
        List<HbtiAnswer> complete = answers(List.of(
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3
        ));

        assertThatThrownBy(() -> engine.score(definition, complete.subList(0, 15)))
                .isInstanceOf(InvalidAssessmentAnswersException.class);

        List<HbtiAnswer> unknown = new ArrayList<>(complete);
        unknown.set(15, new HbtiAnswer("q99", 3));
        assertThatThrownBy(() -> engine.score(definition, unknown))
                .isInstanceOf(InvalidAssessmentAnswersException.class);

        List<HbtiAnswer> duplicate = new ArrayList<>(complete);
        duplicate.set(15, new HbtiAnswer("q1", 3));
        assertThatThrownBy(() -> engine.score(definition, duplicate))
                .isInstanceOf(InvalidAssessmentAnswersException.class);

        List<HbtiAnswer> outOfRange = new ArrayList<>(complete);
        outOfRange.set(0, new HbtiAnswer("q1", 6));
        assertThatThrownBy(() -> engine.score(definition, outOfRange))
                .isInstanceOf(InvalidAssessmentAnswersException.class);
    }

    private HbtiDefinition definition() {
        return repository.findPublished("hbti", "1.0.0").orElseThrow();
    }

    private List<HbtiAnswer> answers(List<Integer> values) {
        List<HbtiAnswer> answers = new ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            answers.add(new HbtiAnswer("q" + (index + 1), values.get(index)));
        }
        return answers;
    }

    private GoldenFixture fixture() throws IOException {
        try (InputStream input = getClass().getResourceAsStream(
                "/fixtures/hbti-scoring-golden-v1.json"
        )) {
            return objectMapper.readValue(input, GoldenFixture.class);
        }
    }

    private record GoldenFixture(
            String sourceCommit,
            String definitionVersion,
            String scoringRuleVersion,
            List<GoldenCase> cases
    ) {
    }

    private record GoldenCase(
            String name,
            List<Integer> values,
            String typeCode,
            List<Integer> leftScores
    ) {
    }
}

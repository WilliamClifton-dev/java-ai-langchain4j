package com.atguigu.java.ai.langchain4j.assessment;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class HbtiScoringEngine {

    public HbtiScoreResult score(HbtiDefinition definition, List<HbtiAnswer> answers) {
        if (definition == null || definition.status() != AssessmentDefinitionStatus.PUBLISHED) {
            throw new InvalidAssessmentAnswersException("A published definition is required");
        }
        Map<String, Integer> values = validateAndIndexAnswers(definition, answers);
        Map<String, List<HbtiItemDefinition>> itemsByDimension = new HashMap<>();
        for (HbtiItemDefinition item : definition.items()) {
            itemsByDimension.computeIfAbsent(item.dimensionCode(), ignored -> new ArrayList<>())
                    .add(item);
        }

        StringBuilder typeCode = new StringBuilder();
        List<HbtiDimensionScore> scores = new ArrayList<>();
        for (HbtiDimensionDefinition dimension : definition.dimensions()) {
            List<HbtiItemDefinition> items = itemsByDimension.getOrDefault(
                    dimension.code(), List.of()
            );
            if (items.isEmpty()) {
                throw new InvalidAssessmentAnswersException("Definition contains an empty dimension");
            }

            double leftTotal = 0;
            for (HbtiItemDefinition item : items) {
                double normalized = (double) (values.get(item.itemKey()) - definition.answerMin())
                        / (definition.answerMax() - definition.answerMin());
                if (item.targetPole().equals(dimension.leftPole())) {
                    leftTotal += normalized;
                } else if (item.targetPole().equals(dimension.rightPole())) {
                    leftTotal += 1 - normalized;
                } else {
                    throw new InvalidAssessmentAnswersException("Definition contains an unknown target pole");
                }
            }

            int leftScore = (int) Math.round(leftTotal / items.size() * 100);
            int rightScore = 100 - leftScore;
            String chosenPole = leftScore >= rightScore
                    ? dimension.leftPole()
                    : dimension.rightPole();
            typeCode.append(chosenPole);
            scores.add(new HbtiDimensionScore(
                    dimension.code(), chosenPole, leftScore, rightScore
            ));
        }

        return new HbtiScoreResult(
                definition.version(), definition.scoringRuleVersion(),
                typeCode.toString(), scores
        );
    }

    private Map<String, Integer> validateAndIndexAnswers(
            HbtiDefinition definition,
            List<HbtiAnswer> answers
    ) {
        if (answers == null) {
            throw new InvalidAssessmentAnswersException("Answers are required");
        }
        Set<String> expectedKeys = new HashSet<>();
        for (HbtiItemDefinition item : definition.items()) {
            if (!expectedKeys.add(item.itemKey())) {
                throw new InvalidAssessmentAnswersException("Definition contains duplicate item keys");
            }
        }

        Map<String, Integer> values = new LinkedHashMap<>();
        for (HbtiAnswer answer : answers) {
            if (answer == null || answer.itemKey() == null
                    || !expectedKeys.contains(answer.itemKey())) {
                throw new InvalidAssessmentAnswersException("Answers contain an unknown item");
            }
            if (values.putIfAbsent(answer.itemKey(), answer.value()) != null) {
                throw new InvalidAssessmentAnswersException("Answers contain a duplicate item");
            }
            if (answer.value() < definition.answerMin()
                    || answer.value() > definition.answerMax()) {
                throw new InvalidAssessmentAnswersException("Answer value is outside the definition range");
            }
        }
        if (!values.keySet().equals(expectedKeys)) {
            throw new InvalidAssessmentAnswersException("A complete answer set is required");
        }
        return values;
    }
}

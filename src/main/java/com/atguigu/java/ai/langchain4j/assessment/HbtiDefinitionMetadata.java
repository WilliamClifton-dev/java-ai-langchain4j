package com.atguigu.java.ai.langchain4j.assessment;

import java.time.Instant;

public record HbtiDefinitionMetadata(
        String id,
        String assessmentKey,
        String version,
        String scoringRuleVersion,
        String displayName,
        AssessmentDefinitionStatus status,
        int answerMin,
        int answerMax,
        String sourceRepository,
        String sourceCommit,
        String sourceContentHash,
        Instant publishedAt
) {
}

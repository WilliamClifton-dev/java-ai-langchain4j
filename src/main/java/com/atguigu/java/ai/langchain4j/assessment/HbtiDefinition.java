package com.atguigu.java.ai.langchain4j.assessment;

import java.time.Instant;
import java.util.List;

public record HbtiDefinition(
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
        Instant publishedAt,
        List<HbtiDimensionDefinition> dimensions,
        List<HbtiItemDefinition> items
) {
    public HbtiDefinition {
        dimensions = List.copyOf(dimensions);
        items = List.copyOf(items);
    }

    static HbtiDefinition from(
            HbtiDefinitionMetadata metadata,
            List<HbtiDimensionDefinition> dimensions,
            List<HbtiItemDefinition> items
    ) {
        return new HbtiDefinition(
                metadata.id(), metadata.assessmentKey(), metadata.version(),
                metadata.scoringRuleVersion(), metadata.displayName(), metadata.status(),
                metadata.answerMin(), metadata.answerMax(), metadata.sourceRepository(),
                metadata.sourceCommit(), metadata.sourceContentHash(), metadata.publishedAt(),
                dimensions, items
        );
    }
}

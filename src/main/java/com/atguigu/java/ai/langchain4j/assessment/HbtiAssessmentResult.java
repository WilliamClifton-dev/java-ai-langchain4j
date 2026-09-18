package com.atguigu.java.ai.langchain4j.assessment;

import java.time.Instant;
import java.util.List;

public record HbtiAssessmentResult(
        String id,
        String definitionVersion,
        String scoringRuleVersion,
        String typeCode,
        List<HbtiDimensionScore> dimensions,
        Instant completedAt
) {
    public HbtiAssessmentResult {
        dimensions = List.copyOf(dimensions);
    }
}

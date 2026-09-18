package com.atguigu.java.ai.langchain4j.assessment;

import java.util.List;

public record HbtiScoreResult(
        String definitionVersion,
        String scoringRuleVersion,
        String typeCode,
        List<HbtiDimensionScore> dimensions
) {
    public HbtiScoreResult {
        dimensions = List.copyOf(dimensions);
    }
}

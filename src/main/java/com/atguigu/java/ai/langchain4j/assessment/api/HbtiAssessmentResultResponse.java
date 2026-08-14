package com.atguigu.java.ai.langchain4j.assessment.api;

import com.atguigu.java.ai.langchain4j.assessment.HbtiAssessmentResult;
import com.atguigu.java.ai.langchain4j.assessment.HbtiDimensionScore;

import java.time.Instant;
import java.util.List;

public record HbtiAssessmentResultResponse(
        String id,
        String definitionVersion,
        String scoringRuleVersion,
        List<HbtiDimensionScore> dimensions,
        String typeCode,
        String limitation,
        Instant completedAt
) {
    private static final String LIMITATION =
            "HBTI is an exploratory behavioral tendency assessment, not a diagnosis.";

    static HbtiAssessmentResultResponse from(HbtiAssessmentResult result) {
        return new HbtiAssessmentResultResponse(
                result.id(), result.definitionVersion(), result.scoringRuleVersion(),
                result.dimensions(), result.typeCode(), LIMITATION, result.completedAt()
        );
    }
}

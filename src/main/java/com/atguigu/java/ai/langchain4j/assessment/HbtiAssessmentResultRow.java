package com.atguigu.java.ai.langchain4j.assessment;

import java.time.Instant;

record HbtiAssessmentResultRow(
        String id,
        String definitionVersion,
        String scoringRuleVersion,
        String payloadHash,
        String typeCode,
        Instant completedAt
) {
}

package com.atguigu.java.ai.langchain4j.planning;

import java.math.BigDecimal;
import java.time.Instant;

public record WeightPlanVersion(
        String id,
        String planId,
        int versionNo,
        PlanVersionStatus status,
        WeightGoal goal,
        Instant profileUpdatedAt,
        String screeningId,
        int screeningVersion,
        String assessmentAttemptId,
        String formulaVersion,
        String targetPolicyVersion,
        BigDecimal bmi,
        int bmrKcalPerDay,
        int tdeeKcalPerDay,
        int energyMinKcalPerDay,
        int energyMaxKcalPerDay,
        BigDecimal weeklyWeightChangeMinPercent,
        BigDecimal weeklyWeightChangeMaxPercent,
        Instant createdAt,
        Instant validatedAt,
        Instant confirmedAt,
        Instant activatedAt,
        Instant replacedAt
) {
}

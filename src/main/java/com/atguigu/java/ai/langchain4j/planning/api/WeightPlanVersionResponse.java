package com.atguigu.java.ai.langchain4j.planning.api;

import com.atguigu.java.ai.langchain4j.planning.PlanVersionStatus;
import com.atguigu.java.ai.langchain4j.planning.WeightGoal;
import com.atguigu.java.ai.langchain4j.planning.WeightPlanVersion;

import java.math.BigDecimal;
import java.time.Instant;

public record WeightPlanVersionResponse(
        String id,
        String planId,
        int versionNo,
        PlanVersionStatus status,
        WeightGoal goal,
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
        Instant replacedAt,
        String guidance
) {
    private static final String GUIDANCE =
            "Targets are planning estimates, not medical prescriptions or guaranteed outcomes.";

    static WeightPlanVersionResponse from(WeightPlanVersion version) {
        return new WeightPlanVersionResponse(
                version.id(), version.planId(), version.versionNo(), version.status(), version.goal(),
                version.formulaVersion(), version.targetPolicyVersion(), version.bmi(),
                version.bmrKcalPerDay(), version.tdeeKcalPerDay(),
                version.energyMinKcalPerDay(), version.energyMaxKcalPerDay(),
                version.weeklyWeightChangeMinPercent(), version.weeklyWeightChangeMaxPercent(),
                version.createdAt(), version.validatedAt(), version.confirmedAt(),
                version.activatedAt(), version.replacedAt(), GUIDANCE
        );
    }
}

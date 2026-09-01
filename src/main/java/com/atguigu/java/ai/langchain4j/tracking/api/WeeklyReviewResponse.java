package com.atguigu.java.ai.langchain4j.tracking.api;

import com.atguigu.java.ai.langchain4j.tracking.WeeklyReview;
import com.atguigu.java.ai.langchain4j.tracking.WeeklyReviewRecommendation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record WeeklyReviewResponse(
        String id, String planVersionId, LocalDate windowStart, LocalDate windowEnd,
        int versionNo, String policyVersion, int weightObservationDays,
        int nutritionLoggedDays, int stepsObservedDays, int sleepObservedDays,
        int trainingDays, BigDecimal averageWeightKg, BigDecimal weightTrendPercent,
        Integer nutritionAdherencePercent, Integer averageSteps, Integer averageSleepMinutes,
        int totalTrainingMinutes, WeeklyReviewRecommendation recommendation,
        int proposedEnergyDeltaKcalPerDay, String reason, Instant createdAt, String limitation
) {
    private static final String LIMITATION = "This deterministic review is a planning aid, "
            + "not medical advice or a diagnosis. Proposed changes are not applied automatically.";

    static WeeklyReviewResponse from(WeeklyReview value) {
        return new WeeklyReviewResponse(
                value.id(), value.planVersionId(), value.windowStart(), value.windowEnd(),
                value.versionNo(), value.policyVersion(), value.weightObservationDays(),
                value.nutritionLoggedDays(), value.stepsObservedDays(), value.sleepObservedDays(),
                value.trainingDays(), value.averageWeightKg(), value.weightTrendPercent(),
                value.nutritionAdherencePercent(), value.averageSteps(),
                value.averageSleepMinutes(), value.totalTrainingMinutes(), value.recommendation(),
                value.proposedEnergyDeltaKcalPerDay(), value.reason(), value.createdAt(), LIMITATION
        );
    }
}

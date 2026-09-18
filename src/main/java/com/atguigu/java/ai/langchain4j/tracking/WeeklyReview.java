package com.atguigu.java.ai.langchain4j.tracking;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record WeeklyReview(
        String id,
        String planVersionId,
        LocalDate windowStart,
        LocalDate windowEnd,
        int versionNo,
        String policyVersion,
        int weightObservationDays,
        int nutritionLoggedDays,
        int stepsObservedDays,
        int sleepObservedDays,
        int trainingDays,
        BigDecimal averageWeightKg,
        BigDecimal weightTrendPercent,
        Integer nutritionAdherencePercent,
        Integer averageSteps,
        Integer averageSleepMinutes,
        int totalTrainingMinutes,
        WeeklyReviewRecommendation recommendation,
        int proposedEnergyDeltaKcalPerDay,
        String reason,
        Instant createdAt
) { }

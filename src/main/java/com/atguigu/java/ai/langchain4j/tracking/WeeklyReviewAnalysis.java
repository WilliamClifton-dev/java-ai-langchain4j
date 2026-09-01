package com.atguigu.java.ai.langchain4j.tracking;

import java.math.BigDecimal;

public record WeeklyReviewAnalysis(
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
        String reason
) { }

package com.atguigu.java.ai.langchain4j.tracking.api;

import com.atguigu.java.ai.langchain4j.tracking.DailyMetric;
import com.atguigu.java.ai.langchain4j.tracking.DailySummary;
import com.atguigu.java.ai.langchain4j.tracking.NutritionLog;
import com.atguigu.java.ai.langchain4j.tracking.TrainingLog;

import java.time.LocalDate;
import java.util.List;

public record DailySummaryResponse(LocalDate localDate, DailyMetric metric,
                                   NutritionLog nutrition, List<TrainingLog> trainingSessions,
                                   int trainingMinutes) {
    static DailySummaryResponse from(DailySummary summary) {
        return new DailySummaryResponse(summary.localDate(), summary.metric().orElse(null),
                summary.nutrition().orElse(null), summary.trainingSessions(),
                summary.trainingMinutes());
    }
}

package com.atguigu.java.ai.langchain4j.tracking;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public record DailySummary(LocalDate localDate, Optional<DailyMetric> metric,
                           Optional<NutritionLog> nutrition, List<TrainingLog> trainingSessions,
                           int trainingMinutes) {
    public DailySummary { trainingSessions = List.copyOf(trainingSessions); }
}

package com.atguigu.java.ai.langchain4j.tracking;

import com.atguigu.java.ai.langchain4j.planning.WeightGoal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record WeeklyReviewInput(
        LocalDate windowStart,
        LocalDate windowEnd,
        WeightGoal goal,
        int energyMinKcalPerDay,
        int energyMaxKcalPerDay,
        BigDecimal weeklyWeightChangeMinPercent,
        BigDecimal weeklyWeightChangeMaxPercent,
        List<WeightObservation> weights,
        List<Integer> energyKcal,
        List<Integer> steps,
        List<Integer> sleepMinutes,
        int totalTrainingMinutes,
        int trainingDays
) {
    public WeeklyReviewInput {
        weights = List.copyOf(weights);
        energyKcal = List.copyOf(energyKcal);
        steps = List.copyOf(steps);
        sleepMinutes = List.copyOf(sleepMinutes);
    }
}

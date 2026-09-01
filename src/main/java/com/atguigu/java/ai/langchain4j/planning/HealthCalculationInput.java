package com.atguigu.java.ai.langchain4j.planning;

import com.atguigu.java.ai.langchain4j.profile.ActivityLevel;
import com.atguigu.java.ai.langchain4j.profile.CalculationSex;

public record HealthCalculationInput(
        int ageYears,
        CalculationSex calculationSex,
        double heightCm,
        double weightKg,
        ActivityLevel activityLevel
) {
}

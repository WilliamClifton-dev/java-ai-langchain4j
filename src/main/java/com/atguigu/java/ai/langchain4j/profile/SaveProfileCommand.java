package com.atguigu.java.ai.langchain4j.profile;

import java.time.LocalDate;

public record SaveProfileCommand(
        LocalDate dateOfBirth,
        CalculationSex calculationSex,
        double heightCm,
        double currentWeightKg,
        double targetWeightKg,
        ActivityLevel activityLevel,
        String timeZone
) {
}

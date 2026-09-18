package com.atguigu.java.ai.langchain4j.profile;

import java.time.Instant;
import java.time.LocalDate;

public record UserProfile(
        String userId,
        LocalDate dateOfBirth,
        CalculationSex calculationSex,
        double heightCm,
        double currentWeightKg,
        double targetWeightKg,
        ActivityLevel activityLevel,
        String timeZone,
        int screeningVersion,
        Instant createdAt,
        Instant updatedAt
) {
}

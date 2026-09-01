package com.atguigu.java.ai.langchain4j.profile.api;

import com.atguigu.java.ai.langchain4j.profile.ActivityLevel;
import com.atguigu.java.ai.langchain4j.profile.CalculationSex;
import com.atguigu.java.ai.langchain4j.profile.UserProfile;

import java.time.LocalDate;

public record ProfileResponse(
        String userId,
        LocalDate dateOfBirth,
        CalculationSex calculationSex,
        double heightCm,
        double currentWeightKg,
        double targetWeightKg,
        ActivityLevel activityLevel,
        String timeZone
) {
    static ProfileResponse from(UserProfile profile) {
        return new ProfileResponse(
                profile.userId(), profile.dateOfBirth(), profile.calculationSex(),
                profile.heightCm(), profile.currentWeightKg(), profile.targetWeightKg(),
                profile.activityLevel(), profile.timeZone()
        );
    }
}

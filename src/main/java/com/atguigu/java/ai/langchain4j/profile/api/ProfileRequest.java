package com.atguigu.java.ai.langchain4j.profile.api;

import com.atguigu.java.ai.langchain4j.profile.ActivityLevel;
import com.atguigu.java.ai.langchain4j.profile.CalculationSex;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ProfileRequest(
        @NotNull @Past LocalDate dateOfBirth,
        @NotNull CalculationSex calculationSex,
        @DecimalMin("100") @DecimalMax("250") double heightCm,
        @DecimalMin("30") @DecimalMax("350") double currentWeightKg,
        @DecimalMin("30") @DecimalMax("350") double targetWeightKg,
        @NotNull ActivityLevel activityLevel,
        @NotBlank @Size(max = 64) String timeZone
) {
}

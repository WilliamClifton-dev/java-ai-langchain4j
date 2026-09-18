package com.atguigu.java.ai.langchain4j.tracking.api;

import com.atguigu.java.ai.langchain4j.tracking.TrainingCommand;
import com.atguigu.java.ai.langchain4j.tracking.TrainingIntensity;
import com.atguigu.java.ai.langchain4j.tracking.TrainingType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TrainingRequest(
        @NotNull LocalDate localDate,
        @NotNull TrainingType trainingType,
        @NotNull @Min(1) @Max(600) Integer durationMinutes,
        @NotNull TrainingIntensity intensity
) {
    TrainingCommand toCommand() {
        return new TrainingCommand(localDate, trainingType, durationMinutes, intensity);
    }
}

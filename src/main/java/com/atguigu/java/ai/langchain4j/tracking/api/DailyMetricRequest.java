package com.atguigu.java.ai.langchain4j.tracking.api;

import com.atguigu.java.ai.langchain4j.tracking.DailyMetricCommand;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyMetricRequest(
        @NotNull LocalDate localDate,
        @DecimalMin("30") @DecimalMax("350") BigDecimal weightKg,
        @Min(0) @Max(100000) Integer steps,
        @Min(0) @Max(1440) Integer activityMinutes,
        @Min(0) @Max(1440) Integer sleepMinutes,
        @Min(1) @Max(5) Integer sleepQuality
) {
    DailyMetricCommand toCommand() {
        return new DailyMetricCommand(localDate, weightKg, steps, activityMinutes,
                sleepMinutes, sleepQuality);
    }
}

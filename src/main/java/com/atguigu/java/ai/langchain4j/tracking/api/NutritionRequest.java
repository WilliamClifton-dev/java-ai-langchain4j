package com.atguigu.java.ai.langchain4j.tracking.api;

import com.atguigu.java.ai.langchain4j.tracking.NutritionCommand;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NutritionRequest(
        @NotNull LocalDate localDate,
        @NotNull @Min(0) @Max(10000) Integer energyKcal,
        @NotNull @DecimalMin("0") @DecimalMax("1000") BigDecimal proteinG,
        @NotNull @DecimalMin("0") @DecimalMax("1000") BigDecimal carbohydrateG,
        @NotNull @DecimalMin("0") @DecimalMax("1000") BigDecimal fatG
) {
    NutritionCommand toCommand() {
        return new NutritionCommand(localDate, energyKcal, proteinG, carbohydrateG, fatG);
    }
}

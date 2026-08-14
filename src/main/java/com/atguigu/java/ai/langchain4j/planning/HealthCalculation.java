package com.atguigu.java.ai.langchain4j.planning;

import java.math.BigDecimal;

public record HealthCalculation(
        BigDecimal bmi,
        int bmrKcalPerDay,
        int tdeeKcalPerDay,
        String formulaVersion
) {
}

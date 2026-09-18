package com.atguigu.java.ai.langchain4j.planning;

import java.math.BigDecimal;

public record TargetRange(
        WeightGoal goal,
        int energyMinKcalPerDay,
        int energyMaxKcalPerDay,
        BigDecimal weeklyWeightChangeMinPercent,
        BigDecimal weeklyWeightChangeMaxPercent,
        String policyVersion
) {
}

package com.atguigu.java.ai.langchain4j.planning;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class TargetRangePolicy {

    static final String POLICY_VERSION = "CONSERVATIVE_ENERGY_RANGE_V1";

    public TargetRange propose(WeightGoal goal, int bmrKcalPerDay, int tdeeKcalPerDay) {
        if (goal == null || bmrKcalPerDay < 800 || tdeeKcalPerDay < bmrKcalPerDay) {
            throw new InvalidHealthCalculationException("Energy inputs are outside supported bounds");
        }
        return switch (goal) {
            case LOSS -> range(
                    goal,
                    Math.max(bmrKcalPerDay, percentage(tdeeKcalPerDay, "0.80")),
                    Math.max(bmrKcalPerDay, percentage(tdeeKcalPerDay, "0.90")),
                    "-0.75", "-0.25"
            );
            case MAINTENANCE -> range(
                    goal,
                    percentage(tdeeKcalPerDay, "0.97"),
                    percentage(tdeeKcalPerDay, "1.03"),
                    "-0.10", "0.10"
            );
            case GAIN -> range(
                    goal,
                    percentage(tdeeKcalPerDay, "1.05"),
                    percentage(tdeeKcalPerDay, "1.10"),
                    "0.10", "0.25"
            );
        };
    }

    private TargetRange range(
            WeightGoal goal,
            int energyMin,
            int energyMax,
            String weeklyMin,
            String weeklyMax
    ) {
        return new TargetRange(
                goal, energyMin, energyMax,
                new BigDecimal(weeklyMin), new BigDecimal(weeklyMax), POLICY_VERSION
        );
    }

    private int percentage(int value, String factor) {
        return BigDecimal.valueOf(value).multiply(new BigDecimal(factor))
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();
    }
}

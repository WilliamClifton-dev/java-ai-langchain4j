package com.atguigu.java.ai.langchain4j.planning;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TargetRangePolicyTest {

    private final TargetRangePolicy policy = new TargetRangePolicy();

    @Test
    void createsBoundedEnergyAndWeeklyChangeRanges() {
        TargetRange loss = policy.propose(WeightGoal.LOSS, 1400, 2000);
        TargetRange maintenance = policy.propose(WeightGoal.MAINTENANCE, 1400, 2000);
        TargetRange gain = policy.propose(WeightGoal.GAIN, 1400, 2000);

        assertThat(loss.energyMinKcalPerDay()).isEqualTo(1600);
        assertThat(loss.energyMaxKcalPerDay()).isEqualTo(1800);
        assertThat(loss.weeklyWeightChangeMinPercent())
                .isEqualByComparingTo(new BigDecimal("-0.75"));
        assertThat(loss.weeklyWeightChangeMaxPercent())
                .isEqualByComparingTo(new BigDecimal("-0.25"));

        assertThat(maintenance.energyMinKcalPerDay()).isEqualTo(1940);
        assertThat(maintenance.energyMaxKcalPerDay()).isEqualTo(2060);
        assertThat(gain.energyMinKcalPerDay()).isEqualTo(2100);
        assertThat(gain.energyMaxKcalPerDay()).isEqualTo(2200);
    }

    @Test
    void lossRangeNeverDropsBelowCalculatedBmr() {
        TargetRange range = policy.propose(WeightGoal.LOSS, 1750, 2000);

        assertThat(range.energyMinKcalPerDay()).isEqualTo(1750);
        assertThat(range.energyMaxKcalPerDay()).isEqualTo(1800);
    }
}

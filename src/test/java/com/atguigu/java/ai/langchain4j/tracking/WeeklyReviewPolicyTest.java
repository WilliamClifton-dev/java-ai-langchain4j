package com.atguigu.java.ai.langchain4j.tracking;

import com.atguigu.java.ai.langchain4j.planning.WeightGoal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyReviewPolicyTest {
    private final WeeklyReviewPolicy policy = new WeeklyReviewPolicy();
    private final LocalDate start = LocalDate.of(2026, 8, 3);

    @Test
    void proposesAConservativelyBoundedIncreaseWhenLossIsTooFast() {
        WeeklyReviewAnalysis result = policy.analyze(input(
                List.of(weight(0, "70.0"), weight(2, "69.4"), weight(4, "68.8"), weight(6, "68.2")),
                List.of(1700, 1750, 1725, 1780, 1760, 1740, 1770)
        ));

        assertThat(result.weightTrendPercent()).isLessThan(new BigDecimal("-0.75"));
        assertThat(result.nutritionAdherencePercent()).isEqualTo(100);
        assertThat(result.recommendation()).isEqualTo(WeeklyReviewRecommendation.INCREASE_ENERGY);
        assertThat(result.proposedEnergyDeltaKcalPerDay()).isEqualTo(100);
    }

    @Test
    void holdsThePlanWhenObservedTrendIsInsideThePlanRange() {
        WeeklyReviewAnalysis result = policy.analyze(input(
                List.of(weight(0, "70.0"), weight(3, "69.85"), weight(6, "69.70")),
                List.of(1700, 1750, 1725, 1780)
        ));

        assertThat(result.weightTrendPercent()).isBetween(
                new BigDecimal("-0.75"), new BigDecimal("-0.25"));
        assertThat(result.recommendation()).isEqualTo(WeeklyReviewRecommendation.HOLD);
        assertThat(result.proposedEnergyDeltaKcalPerDay()).isZero();
    }

    private WeeklyReviewInput input(List<WeightObservation> weights, List<Integer> energy) {
        return new WeeklyReviewInput(
                start, start.plusDays(6), WeightGoal.LOSS, 1600, 1800,
                new BigDecimal("-0.75"), new BigDecimal("-0.25"), weights,
                energy, List.of(8000, 7000, 9000), List.of(420, 450, 480), 180, 3
        );
    }

    private WeightObservation weight(int day, String kg) {
        return new WeightObservation(start.plusDays(day), new BigDecimal(kg));
    }
}

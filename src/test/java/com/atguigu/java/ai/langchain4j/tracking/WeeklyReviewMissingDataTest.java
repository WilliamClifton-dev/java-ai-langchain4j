package com.atguigu.java.ai.langchain4j.tracking;

import com.atguigu.java.ai.langchain4j.planning.WeightGoal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyReviewMissingDataTest {
    @Test
    void rejectsSingleDayNoiseAndPreservesUnknownCoverage() {
        LocalDate start = LocalDate.of(2026, 8, 3);
        WeeklyReviewAnalysis result = new WeeklyReviewPolicy().analyze(new WeeklyReviewInput(
                start, start.plusDays(6), WeightGoal.LOSS, 1600, 1800,
                new BigDecimal("-0.75"), new BigDecimal("-0.25"),
                List.of(new WeightObservation(start, new BigDecimal("70"))),
                List.of(), List.of(), List.of(), 0, 0
        ));

        assertThat(result.weightObservationDays()).isEqualTo(1);
        assertThat(result.weightTrendPercent()).isNull();
        assertThat(result.nutritionLoggedDays()).isZero();
        assertThat(result.nutritionAdherencePercent()).isNull();
        assertThat(result.averageSteps()).isNull();
        assertThat(result.averageSleepMinutes()).isNull();
        assertThat(result.recommendation()).isEqualTo(WeeklyReviewRecommendation.INSUFFICIENT_DATA);
        assertThat(result.proposedEnergyDeltaKcalPerDay()).isZero();
    }
}

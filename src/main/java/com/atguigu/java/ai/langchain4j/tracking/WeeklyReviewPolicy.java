package com.atguigu.java.ai.langchain4j.tracking;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Component
public class WeeklyReviewPolicy {
    public static final String POLICY_VERSION = "DETERMINISTIC_WEEKLY_REVIEW_V1";
    private static final int MIN_WEIGHT_DAYS = 3;
    private static final int MIN_NUTRITION_DAYS = 4;
    private static final int MIN_ADHERENCE_PERCENT = 75;
    private static final int ENERGY_STEP_KCAL = 100;

    public WeeklyReviewAnalysis analyze(WeeklyReviewInput input) {
        validate(input);
        List<WeightObservation> weights = input.weights().stream()
                .sorted(Comparator.comparing(WeightObservation::localDate)).toList();
        BigDecimal averageWeight = averageDecimal(
                weights.stream().map(WeightObservation::weightKg).toList(), 2
        );
        BigDecimal trend = weights.size() < MIN_WEIGHT_DAYS
                ? null : weeklyTrend(input, weights, averageWeight);
        Integer adherence = input.energyKcal().isEmpty() ? null : adherence(input);
        Recommendation recommendation = recommendation(input, trend, adherence);

        return new WeeklyReviewAnalysis(
                weights.size(), input.energyKcal().size(), input.steps().size(),
                input.sleepMinutes().size(), input.trainingDays(), averageWeight, trend,
                adherence, averageInteger(input.steps()), averageInteger(input.sleepMinutes()),
                input.totalTrainingMinutes(), recommendation.value(), recommendation.energyDelta(),
                recommendation.reason()
        );
    }

    private Recommendation recommendation(
            WeeklyReviewInput input, BigDecimal trend, Integer adherence
    ) {
        if (trend == null || input.energyKcal().size() < MIN_NUTRITION_DAYS) {
            return new Recommendation(WeeklyReviewRecommendation.INSUFFICIENT_DATA, 0,
                    "MINIMUM_COVERAGE_NOT_MET");
        }
        if (adherence < MIN_ADHERENCE_PERCENT) {
            return new Recommendation(WeeklyReviewRecommendation.HOLD, 0,
                    "ENERGY_ADHERENCE_TOO_LOW");
        }
        if (trend.compareTo(input.weeklyWeightChangeMinPercent()) < 0) {
            return new Recommendation(WeeklyReviewRecommendation.INCREASE_ENERGY,
                    ENERGY_STEP_KCAL, "TREND_BELOW_PLAN_RANGE");
        }
        if (trend.compareTo(input.weeklyWeightChangeMaxPercent()) > 0) {
            return new Recommendation(WeeklyReviewRecommendation.DECREASE_ENERGY,
                    -ENERGY_STEP_KCAL, "TREND_ABOVE_PLAN_RANGE");
        }
        return new Recommendation(WeeklyReviewRecommendation.HOLD, 0,
                "TREND_WITHIN_PLAN_RANGE");
    }

    private BigDecimal weeklyTrend(
            WeeklyReviewInput input, List<WeightObservation> weights, BigDecimal averageWeight
    ) {
        BigDecimal meanX = weights.stream()
                .map(value -> BigDecimal.valueOf(ChronoUnit.DAYS.between(
                        input.windowStart(), value.localDate())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(weights.size()), 8, RoundingMode.HALF_UP);
        BigDecimal numerator = BigDecimal.ZERO;
        BigDecimal denominator = BigDecimal.ZERO;
        for (WeightObservation observation : weights) {
            BigDecimal x = BigDecimal.valueOf(ChronoUnit.DAYS.between(
                    input.windowStart(), observation.localDate()));
            BigDecimal xDelta = x.subtract(meanX);
            numerator = numerator.add(xDelta.multiply(observation.weightKg().subtract(averageWeight)));
            denominator = denominator.add(xDelta.multiply(xDelta));
        }
        if (denominator.signum() == 0) return null;
        BigDecimal slope = numerator.divide(denominator, 8, RoundingMode.HALF_UP);
        return slope.multiply(BigDecimal.valueOf(7)).divide(averageWeight, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    private Integer adherence(WeeklyReviewInput input) {
        long within = input.energyKcal().stream().filter(value ->
                value >= input.energyMinKcalPerDay() && value <= input.energyMaxKcalPerDay()
        ).count();
        return BigDecimal.valueOf(within * 100L)
                .divide(BigDecimal.valueOf(input.energyKcal().size()), 0, RoundingMode.HALF_UP)
                .intValueExact();
    }

    private BigDecimal averageDecimal(List<BigDecimal> values, int scale) {
        if (values.isEmpty()) return null;
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), scale, RoundingMode.HALF_UP);
    }

    private Integer averageInteger(List<Integer> values) {
        if (values.isEmpty()) return null;
        long sum = values.stream().mapToLong(Integer::longValue).sum();
        return BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(values.size()), 0,
                RoundingMode.HALF_UP).intValueExact();
    }

    private void validate(WeeklyReviewInput input) {
        if (input == null || input.windowStart() == null || input.windowEnd() == null
                || ChronoUnit.DAYS.between(input.windowStart(), input.windowEnd()) != 6
                || input.goal() == null || input.energyMinKcalPerDay() < 0
                || input.energyMaxKcalPerDay() < input.energyMinKcalPerDay()
                || input.weeklyWeightChangeMinPercent() == null
                || input.weeklyWeightChangeMaxPercent() == null
                || input.weeklyWeightChangeMinPercent()
                .compareTo(input.weeklyWeightChangeMaxPercent()) > 0
                || input.totalTrainingMinutes() < 0 || input.trainingDays() < 0
                || input.weights() == null || input.energyKcal() == null
                || input.steps() == null || input.sleepMinutes() == null
                || input.weights().stream().anyMatch(value -> value.localDate() == null
                || value.weightKg() == null || value.localDate().isBefore(input.windowStart())
                || value.localDate().isAfter(input.windowEnd()))) {
            throw new InvalidWeeklyReviewException("Weekly review input is invalid");
        }
    }

    private record Recommendation(
            WeeklyReviewRecommendation value, int energyDelta, String reason
    ) { }
}

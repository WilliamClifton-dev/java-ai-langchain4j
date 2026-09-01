package com.atguigu.java.ai.langchain4j.planning;

import com.atguigu.java.ai.langchain4j.profile.ActivityLevel;
import com.atguigu.java.ai.langchain4j.profile.CalculationSex;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class HealthCalculator {

    static final String FORMULA_VERSION = "MIFFLIN_ST_JEOR_METRIC_V1";

    public HealthCalculation calculate(HealthCalculationInput input) {
        validate(input);
        BigDecimal heightCm = BigDecimal.valueOf(input.heightCm());
        BigDecimal weightKg = BigDecimal.valueOf(input.weightKg());
        BigDecimal heightM = heightCm.movePointLeft(2);
        BigDecimal bmi = weightKg.divide(
                heightM.multiply(heightM), 1, RoundingMode.HALF_UP
        );

        BigDecimal rawBmr = weightKg.multiply(BigDecimal.TEN)
                .add(heightCm.multiply(new BigDecimal("6.25")))
                .subtract(BigDecimal.valueOf(input.ageYears()).multiply(BigDecimal.valueOf(5)))
                .add(input.calculationSex() == CalculationSex.MALE
                        ? BigDecimal.valueOf(5)
                        : BigDecimal.valueOf(-161));
        int bmr = rawBmr.setScale(0, RoundingMode.HALF_UP).intValueExact();
        int tdee = rawBmr.multiply(activityFactor(input.activityLevel()))
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();

        return new HealthCalculation(bmi, bmr, tdee, FORMULA_VERSION);
    }

    private BigDecimal activityFactor(ActivityLevel activityLevel) {
        return switch (activityLevel) {
            case SEDENTARY -> new BigDecimal("1.2");
            case LIGHT -> new BigDecimal("1.375");
            case MODERATE -> new BigDecimal("1.55");
            case VERY_ACTIVE -> new BigDecimal("1.725");
        };
    }

    private void validate(HealthCalculationInput input) {
        if (input == null || input.calculationSex() == null || input.activityLevel() == null
                || input.ageYears() < 18 || input.ageYears() > 120
                || !Double.isFinite(input.heightCm()) || !Double.isFinite(input.weightKg())
                || input.heightCm() < 100 || input.heightCm() > 250
                || input.weightKg() < 30 || input.weightKg() > 350) {
            throw new InvalidHealthCalculationException("Calculation inputs are outside supported bounds");
        }
    }
}

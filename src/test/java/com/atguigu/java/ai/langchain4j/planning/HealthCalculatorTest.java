package com.atguigu.java.ai.langchain4j.planning;

import com.atguigu.java.ai.langchain4j.profile.ActivityLevel;
import com.atguigu.java.ai.langchain4j.profile.CalculationSex;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HealthCalculatorTest {

    private final HealthCalculator calculator = new HealthCalculator();

    @Test
    void calculatesMetricBmiMifflinStJeorBmrAndTdeeWithDeclaredRounding() {
        HealthCalculation male = calculator.calculate(new HealthCalculationInput(
                30, CalculationSex.MALE, 180, 80, ActivityLevel.MODERATE
        ));
        HealthCalculation female = calculator.calculate(new HealthCalculationInput(
                40, CalculationSex.FEMALE, 165, 60, ActivityLevel.SEDENTARY
        ));

        assertThat(male.bmi()).isEqualByComparingTo(new BigDecimal("24.7"));
        assertThat(male.bmrKcalPerDay()).isEqualTo(1780);
        assertThat(male.tdeeKcalPerDay()).isEqualTo(2759);
        assertThat(female.bmi()).isEqualByComparingTo(new BigDecimal("22.0"));
        assertThat(female.bmrKcalPerDay()).isEqualTo(1270);
        assertThat(female.tdeeKcalPerDay()).isEqualTo(1524);
    }

    @Test
    void rejectsUnsupportedOrNonFiniteInputs() {
        assertThatThrownBy(() -> calculator.calculate(new HealthCalculationInput(
                17, CalculationSex.MALE, 180, 80, ActivityLevel.MODERATE
        ))).isInstanceOf(InvalidHealthCalculationException.class);
        assertThatThrownBy(() -> calculator.calculate(new HealthCalculationInput(
                30, CalculationSex.MALE, Double.NaN, 80, ActivityLevel.MODERATE
        ))).isInstanceOf(InvalidHealthCalculationException.class);
    }
}

package com.atguigu.java.ai.langchain4j.profile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SafetyScreeningPolicyTest {

    private final SafetyScreeningPolicy policy = new SafetyScreeningPolicy();
    private final LocalDate today = LocalDate.of(2026, 8, 14);

    @Test
    void allowsAutomaticPlanningForAnAdultWithoutReportedRiskFlags() {
        ScreeningDecision decision = policy.evaluate(
                profile(LocalDate.of(1995, 1, 1)),
                answers(false, false, false, false, false),
                today
        );

        assertThat(decision.status()).isEqualTo(ScreeningStatus.ELIGIBLE);
        assertThat(decision.automaticPlanningAllowed()).isTrue();
        assertThat(decision.reasonCodes()).isEmpty();
    }

    @Test
    void blocksMinorsFromTheAdultProduct() {
        ScreeningDecision decision = policy.evaluate(
                profile(LocalDate.of(2010, 1, 1)),
                answers(false, false, false, false, false),
                today
        );

        assertThat(decision.status()).isEqualTo(ScreeningStatus.INELIGIBLE);
        assertThat(decision.automaticPlanningAllowed()).isFalse();
        assertThat(decision.reasonCodes()).containsExactly(ScreeningReason.UNDER_18);
    }

    @Test
    void routesAnyReportedRiskToProfessionalReviewWithoutDiagnosing() {
        ScreeningDecision decision = policy.evaluate(
                profile(LocalDate.of(1990, 1, 1)),
                answers(false, true, false, false, true),
                today
        );

        assertThat(decision.status()).isEqualTo(ScreeningStatus.PROFESSIONAL_REVIEW);
        assertThat(decision.automaticPlanningAllowed()).isFalse();
        assertThat(decision.reasonCodes())
                .containsExactly(ScreeningReason.EATING_DISORDER_SUPPORT, ScreeningReason.CONCERNING_SYMPTOMS);
    }

    private UserProfile profile(LocalDate dateOfBirth) {
        return new UserProfile(
                "user-1", dateOfBirth, CalculationSex.FEMALE,
                165, 70, 60, ActivityLevel.MODERATE,
                "Asia/Hong_Kong", 0, null, null
        );
    }

    private SafetyScreeningAnswers answers(
            boolean pregnantOrBreastfeeding,
            boolean eatingDisorderHistory,
            boolean medicalGuidanceRequired,
            boolean weightAffectingMedication,
            boolean concerningSymptoms
    ) {
        return new SafetyScreeningAnswers(
                pregnantOrBreastfeeding,
                eatingDisorderHistory,
                medicalGuidanceRequired,
                weightAffectingMedication,
                concerningSymptoms
        );
    }
}

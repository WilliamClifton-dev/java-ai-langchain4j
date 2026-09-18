package com.atguigu.java.ai.langchain4j.planning;

import com.atguigu.java.ai.langchain4j.profile.ActivityLevel;
import com.atguigu.java.ai.langchain4j.profile.CalculationSex;
import com.atguigu.java.ai.langchain4j.profile.SafetyScreening;
import com.atguigu.java.ai.langchain4j.profile.ScreeningStatus;
import com.atguigu.java.ai.langchain4j.profile.UserProfile;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PlanningEligibilityTest {

    private final PlanningEligibilityPolicy policy = new PlanningEligibilityPolicy();
    private final Instant screenedAt = Instant.parse("2026-08-14T08:00:00Z");

    @Test
    void allowsOnlyTheCurrentEligibleScreeningForAnUnchangedProfile() {
        PlanningEligibility decision = policy.evaluate(
                profile(1, screenedAt), screening(1, ScreeningStatus.ELIGIBLE, true)
        );

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.reason()).isEqualTo(PlanningEligibilityReason.ELIGIBLE);
    }

    @Test
    void rejectsMissingRiskBlockedVersionMismatchedAndStaleScreenings() {
        assertThat(policy.evaluate(profile(0, screenedAt), null).reason())
                .isEqualTo(PlanningEligibilityReason.SCREENING_REQUIRED);
        assertThat(policy.evaluate(
                profile(1, screenedAt), screening(1, ScreeningStatus.PROFESSIONAL_REVIEW, false)
        ).reason()).isEqualTo(PlanningEligibilityReason.PROFESSIONAL_REVIEW_REQUIRED);
        assertThat(policy.evaluate(
                profile(2, screenedAt), screening(1, ScreeningStatus.ELIGIBLE, true)
        ).reason()).isEqualTo(PlanningEligibilityReason.SCREENING_STALE);
        assertThat(policy.evaluate(
                profile(1, screenedAt.plusSeconds(1)),
                screening(1, ScreeningStatus.ELIGIBLE, true)
        ).reason()).isEqualTo(PlanningEligibilityReason.SCREENING_STALE);
    }

    private UserProfile profile(int screeningVersion, Instant updatedAt) {
        return new UserProfile(
                "user-1", LocalDate.of(1990, 1, 1), CalculationSex.FEMALE,
                165, 70, 60, ActivityLevel.MODERATE, "Asia/Hong_Kong",
                screeningVersion, updatedAt.minusSeconds(60), updatedAt
        );
    }

    private SafetyScreening screening(
            int version,
            ScreeningStatus status,
            boolean automaticPlanningAllowed
    ) {
        return new SafetyScreening(
                "screen-1", "user-1", version,
                false, false, false, false, false,
                status, automaticPlanningAllowed, "", screenedAt
        );
    }
}

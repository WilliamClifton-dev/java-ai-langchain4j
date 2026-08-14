package com.atguigu.java.ai.langchain4j.planning;

import com.atguigu.java.ai.langchain4j.profile.SafetyScreening;
import com.atguigu.java.ai.langchain4j.profile.ScreeningStatus;
import com.atguigu.java.ai.langchain4j.profile.UserProfile;
import org.springframework.stereotype.Component;

@Component
public class PlanningEligibilityPolicy {

    public PlanningEligibility evaluate(UserProfile profile, SafetyScreening screening) {
        if (profile == null || screening == null) {
            return denied(PlanningEligibilityReason.SCREENING_REQUIRED);
        }
        if (!profile.userId().equals(screening.userId())
                || profile.screeningVersion() != screening.version()
                || screening.createdAt().isBefore(profile.updatedAt())) {
            return denied(PlanningEligibilityReason.SCREENING_STALE);
        }
        if (screening.status() == ScreeningStatus.INELIGIBLE) {
            return denied(PlanningEligibilityReason.NOT_ELIGIBLE);
        }
        if (screening.status() == ScreeningStatus.PROFESSIONAL_REVIEW
                || !screening.automaticPlanningAllowed()) {
            return denied(PlanningEligibilityReason.PROFESSIONAL_REVIEW_REQUIRED);
        }
        return new PlanningEligibility(true, PlanningEligibilityReason.ELIGIBLE);
    }

    private PlanningEligibility denied(PlanningEligibilityReason reason) {
        return new PlanningEligibility(false, reason);
    }
}

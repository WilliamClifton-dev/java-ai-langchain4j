package com.atguigu.java.ai.langchain4j.profile.api;

import com.atguigu.java.ai.langchain4j.profile.SafetyScreening;
import com.atguigu.java.ai.langchain4j.profile.ScreeningReason;
import com.atguigu.java.ai.langchain4j.profile.ScreeningStatus;

import java.time.Instant;
import java.util.List;

public record SafetyScreeningResponse(
        String id,
        int version,
        ScreeningStatus status,
        boolean automaticPlanningAllowed,
        List<ScreeningReason> reasonCodes,
        String guidance,
        Instant createdAt
) {
    static SafetyScreeningResponse from(SafetyScreening screening) {
        String guidance = switch (screening.status()) {
            case ELIGIBLE -> "Automatic planning is available.";
            case PROFESSIONAL_REVIEW ->
                    "Automatic planning is paused. Consider guidance from a qualified professional.";
            case INELIGIBLE -> "This adult weight-management product is not available for this profile.";
        };
        return new SafetyScreeningResponse(
                screening.id(), screening.version(), screening.status(),
                screening.automaticPlanningAllowed(), screening.reasonCodeList(),
                guidance, screening.createdAt()
        );
    }
}

package com.atguigu.java.ai.langchain4j.profile;

import java.time.Instant;
import java.util.List;

public record SafetyScreening(
        String id,
        String userId,
        int version,
        boolean pregnantOrBreastfeeding,
        boolean eatingDisorderHistory,
        boolean medicalGuidanceRequired,
        boolean weightAffectingMedication,
        boolean concerningSymptoms,
        ScreeningStatus status,
        boolean automaticPlanningAllowed,
        String reasonCodes,
        Instant createdAt
) {
    public List<ScreeningReason> reasonCodeList() {
        if (reasonCodes == null || reasonCodes.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(reasonCodes.split(","))
                .map(ScreeningReason::valueOf)
                .toList();
    }
}

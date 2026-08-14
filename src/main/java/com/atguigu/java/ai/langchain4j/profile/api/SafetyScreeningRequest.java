package com.atguigu.java.ai.langchain4j.profile.api;

import jakarta.validation.constraints.NotNull;

public record SafetyScreeningRequest(
        @NotNull Boolean pregnantOrBreastfeeding,
        @NotNull Boolean eatingDisorderHistory,
        @NotNull Boolean medicalGuidanceRequired,
        @NotNull Boolean weightAffectingMedication,
        @NotNull Boolean concerningSymptoms
) {
}

package com.atguigu.java.ai.langchain4j.profile;

public record SafetyScreeningAnswers(
        boolean pregnantOrBreastfeeding,
        boolean eatingDisorderHistory,
        boolean medicalGuidanceRequired,
        boolean weightAffectingMedication,
        boolean concerningSymptoms
) {
}

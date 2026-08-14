package com.atguigu.java.ai.langchain4j.profile;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Component
public class SafetyScreeningPolicy {

    public ScreeningDecision evaluate(
            UserProfile profile,
            SafetyScreeningAnswers answers,
            LocalDate today
    ) {
        List<ScreeningReason> reasons = new ArrayList<>();
        boolean under18 = Period.between(profile.dateOfBirth(), today).getYears() < 18;
        if (under18) {
            reasons.add(ScreeningReason.UNDER_18);
        }
        if (answers.pregnantOrBreastfeeding()) {
            reasons.add(ScreeningReason.PREGNANCY_OR_BREASTFEEDING);
        }
        if (answers.eatingDisorderHistory()) {
            reasons.add(ScreeningReason.EATING_DISORDER_SUPPORT);
        }
        if (answers.medicalGuidanceRequired()) {
            reasons.add(ScreeningReason.MEDICAL_REVIEW);
        }
        if (answers.weightAffectingMedication()) {
            reasons.add(ScreeningReason.WEIGHT_AFFECTING_MEDICATION);
        }
        if (answers.concerningSymptoms()) {
            reasons.add(ScreeningReason.CONCERNING_SYMPTOMS);
        }

        if (under18) {
            return new ScreeningDecision(ScreeningStatus.INELIGIBLE, false, reasons);
        }
        if (!reasons.isEmpty()) {
            return new ScreeningDecision(ScreeningStatus.PROFESSIONAL_REVIEW, false, reasons);
        }
        return new ScreeningDecision(ScreeningStatus.ELIGIBLE, true, List.of());
    }
}

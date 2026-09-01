package com.atguigu.java.ai.langchain4j.identity.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AccountDataExport(
        String version,
        Instant exportedAt,
        Account account,
        Profile profile,
        List<Screening> screenings,
        List<Assessment> assessments,
        List<Plan> plans,
        List<DailyMetric> dailyMetrics,
        List<Nutrition> nutritionLogs,
        List<Training> trainingLogs,
        List<WeeklyReview> weeklyReviews,
        List<Conversation> coachConversations
) {
    public record Account(String id, String email, String status,
                          Instant createdAt, Instant updatedAt) { }

    public record Profile(LocalDate dateOfBirth, String calculationSex,
                          BigDecimal heightCm, BigDecimal currentWeightKg,
                          BigDecimal targetWeightKg, String activityLevel,
                          String timeZone, int screeningVersion,
                          Instant createdAt, Instant updatedAt) { }

    public record Screening(String id, int version, boolean pregnantOrBreastfeeding,
                            boolean eatingDisorderHistory, boolean medicalGuidanceRequired,
                            boolean weightAffectingMedication, boolean concerningSymptoms,
                            String status, boolean automaticPlanningAllowed,
                            String reasonCodes, Instant createdAt) { }

    public record Assessment(String id, String definitionId, String status,
                             String typeCode, Instant createdAt, Instant completedAt,
                             List<Answer> answers, List<Score> scores) { }

    public record Answer(String itemKey, int answerValue) { }

    public record Score(String dimensionCode, int ordinal, String chosenPole,
                        int leftScore, int rightScore) { }

    public record Plan(String id, String activeVersionId, int nextVersionNo,
                       Instant createdAt, Instant updatedAt, List<PlanVersion> versions) { }

    public record PlanVersion(String id, int versionNo, String status, String goal,
                              Instant profileUpdatedAt, String screeningId,
                              int screeningVersion, String assessmentAttemptId,
                              String formulaVersion, String targetPolicyVersion,
                              BigDecimal bmi, int bmrKcalPerDay, int tdeeKcalPerDay,
                              int energyMinKcalPerDay, int energyMaxKcalPerDay,
                              BigDecimal weeklyWeightChangeMinPercent,
                              BigDecimal weeklyWeightChangeMaxPercent,
                              Instant createdAt, Instant validatedAt,
                              Instant confirmedAt, Instant activatedAt,
                              Instant replacedAt) { }

    public record DailyMetric(String id, LocalDate localDate, BigDecimal weightKg,
                              Integer steps, Integer activityMinutes, Integer sleepMinutes,
                              Integer sleepQuality, Instant createdAt) { }

    public record Nutrition(String id, LocalDate localDate, int energyKcal,
                            BigDecimal proteinG, BigDecimal carbohydrateG,
                            BigDecimal fatG, Instant createdAt) { }

    public record Training(String id, LocalDate localDate, String trainingType,
                           int durationMinutes, String intensity, Instant createdAt) { }

    public record WeeklyReview(String id, String planVersionId, LocalDate windowStart,
                               LocalDate windowEnd, int versionNo, String policyVersion,
                               int weightObservationDays, int nutritionLoggedDays,
                               int stepsObservedDays, int sleepObservedDays,
                               int trainingDays, BigDecimal averageWeightKg,
                               BigDecimal weightTrendPercent, Integer nutritionAdherencePercent,
                               Integer averageSteps, Integer averageSleepMinutes,
                               int totalTrainingMinutes, String recommendation,
                               int proposedEnergyDeltaKcal, String reason,
                               Instant createdAt) { }

    public record Conversation(String id, Instant createdAt, Instant updatedAt,
                               List<Message> messages) { }

    public record Message(long id, int sequenceNo, String messageJson,
                          Instant createdAt) { }
}

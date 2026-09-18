package com.atguigu.java.ai.langchain4j.identity;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Mapper
public interface AccountDataLifecycleMapper {

    @Select("""
            SELECT id, normalized_email AS email, status, created_at, updated_at
            FROM user_account WHERE id = #{userId}
            """)
    Optional<AccountRow> findAccount(String userId);

    @Select("""
            SELECT date_of_birth, calculation_sex, height_cm, current_weight_kg,
                   target_weight_kg, activity_level, time_zone, screening_version,
                   created_at, updated_at
            FROM user_profile WHERE user_id = #{userId}
            """)
    Optional<ProfileRow> findProfile(String userId);

    @Select("""
            SELECT id, version, pregnant_or_breastfeeding, eating_disorder_history,
                   medical_guidance_required, weight_affecting_medication,
                   concerning_symptoms, status, automatic_planning_allowed,
                   reason_codes, created_at
            FROM safety_screening WHERE user_id = #{userId}
            ORDER BY version, id LIMIT 1001
            """)
    List<ScreeningRow> findScreenings(String userId);

    @Select("""
            SELECT id, definition_id, status, type_code, created_at, completed_at
            FROM assessment_attempt WHERE user_id = #{userId}
            ORDER BY completed_at, id LIMIT 1001
            """)
    List<AssessmentRow> findAssessments(String userId);

    @Select("""
            SELECT answer.attempt_id, answer.item_key, answer.answer_value
            FROM assessment_answer answer
            JOIN assessment_attempt attempt ON attempt.id = answer.attempt_id
            WHERE attempt.user_id = #{userId}
            ORDER BY answer.attempt_id, answer.item_key LIMIT 10001
            """)
    List<AnswerRow> findAnswers(String userId);

    @Select("""
            SELECT score.attempt_id, score.dimension_code, score.ordinal,
                   score.chosen_pole, score.left_score, score.right_score
            FROM assessment_score score
            JOIN assessment_attempt attempt ON attempt.id = score.attempt_id
            WHERE attempt.user_id = #{userId}
            ORDER BY score.attempt_id, score.ordinal LIMIT 10001
            """)
    List<ScoreRow> findScores(String userId);

    @Select("""
            SELECT id, active_version_id, next_version_no, created_at, updated_at
            FROM weight_plan WHERE user_id = #{userId}
            """)
    List<PlanRow> findPlans(String userId);

    @Select("""
            SELECT version.id, version.plan_id, version.version_no, version.status,
                   version.goal, version.profile_updated_at, version.screening_id,
                   version.screening_version, version.assessment_attempt_id,
                   version.formula_version, version.target_policy_version, version.bmi,
                   version.bmr_kcal_per_day, version.tdee_kcal_per_day,
                   version.energy_min_kcal_per_day, version.energy_max_kcal_per_day,
                   version.weekly_weight_change_min_percent,
                   version.weekly_weight_change_max_percent, version.created_at,
                   version.validated_at, version.confirmed_at, version.activated_at,
                   version.replaced_at
            FROM weight_plan_version version
            JOIN weight_plan plan ON plan.id = version.plan_id
            WHERE plan.user_id = #{userId}
            ORDER BY version.plan_id, version.version_no LIMIT 1001
            """)
    List<PlanVersionRow> findPlanVersions(String userId);

    @Select("""
            SELECT id, local_date, weight_kg, steps, activity_minutes, sleep_minutes,
                   sleep_quality, created_at
            FROM daily_metric WHERE user_id = #{userId}
            ORDER BY local_date, id LIMIT 1001
            """)
    List<DailyMetricRow> findDailyMetrics(String userId);

    @Select("""
            SELECT id, local_date, energy_kcal, protein_g, carbohydrate_g, fat_g,
                   created_at
            FROM nutrition_log WHERE user_id = #{userId}
            ORDER BY local_date, id LIMIT 1001
            """)
    List<NutritionRow> findNutritionLogs(String userId);

    @Select("""
            SELECT id, local_date, training_type, duration_minutes, intensity, created_at
            FROM training_log WHERE user_id = #{userId}
            ORDER BY local_date, created_at, id LIMIT 1001
            """)
    List<TrainingRow> findTrainingLogs(String userId);

    @Select("""
            SELECT review.id, review.plan_version_id, review.window_start,
                   review.window_end, review.version_no, review.policy_version,
                   review.weight_observation_days, review.nutrition_logged_days,
                   review.steps_observed_days, review.sleep_observed_days,
                   review.training_days, review.average_weight_kg,
                   review.weight_trend_percent, review.nutrition_adherence_percent,
                   review.average_steps, review.average_sleep_minutes,
                   review.total_training_minutes, review.recommendation,
                   review.proposed_energy_delta_kcal, review.reason, review.created_at
            FROM weekly_review review WHERE review.user_id = #{userId}
            ORDER BY review.window_end, review.version_no, review.id LIMIT 1001
            """)
    List<WeeklyReviewRow> findWeeklyReviews(String userId);

    @Select("""
            SELECT id, created_at, updated_at
            FROM coach_conversation WHERE user_id = #{userId}
            ORDER BY updated_at, id LIMIT 1001
            """)
    List<ConversationRow> findConversations(String userId);

    @Select("""
            SELECT message.id, message.conversation_id, message.sequence_no,
                   message.message_json, message.created_at
            FROM coach_message message
            JOIN coach_conversation conversation
              ON conversation.id = message.conversation_id
            WHERE conversation.user_id = #{userId}
            ORDER BY message.conversation_id, message.sequence_no LIMIT 10001
            """)
    List<MessageRow> findMessages(String userId);

    @Update("UPDATE audit_event SET user_id = NULL WHERE user_id = #{userId}")
    int anonymizeAuditEvents(String userId);

    @Delete("DELETE FROM weekly_review WHERE user_id = #{userId}")
    int deleteWeeklyReviews(String userId);

    @Delete("""
            DELETE FROM weight_plan_version
            WHERE plan_id IN (SELECT id FROM weight_plan WHERE user_id = #{userId})
            """)
    int deletePlanVersions(String userId);

    @Delete("DELETE FROM weight_plan WHERE user_id = #{userId}")
    int deletePlans(String userId);

    @Delete("DELETE FROM user_account WHERE id = #{userId}")
    int deleteAccount(String userId);

    record AccountRow(String id, String email, AccountStatus status,
                      Instant createdAt, Instant updatedAt) { }

    record ProfileRow(LocalDate dateOfBirth, String calculationSex, BigDecimal heightCm,
                      BigDecimal currentWeightKg, BigDecimal targetWeightKg,
                      String activityLevel, String timeZone, int screeningVersion,
                      Instant createdAt, Instant updatedAt) { }

    record ScreeningRow(String id, int version, boolean pregnantOrBreastfeeding,
                        boolean eatingDisorderHistory, boolean medicalGuidanceRequired,
                        boolean weightAffectingMedication, boolean concerningSymptoms,
                        String status, boolean automaticPlanningAllowed, String reasonCodes,
                        Instant createdAt) { }

    record AssessmentRow(String id, String definitionId, String status, String typeCode,
                         Instant createdAt, Instant completedAt) { }

    record AnswerRow(String attemptId, String itemKey, int answerValue) { }

    record ScoreRow(String attemptId, String dimensionCode, int ordinal, String chosenPole,
                    int leftScore, int rightScore) { }

    record PlanRow(String id, String activeVersionId, int nextVersionNo,
                   Instant createdAt, Instant updatedAt) { }

    record PlanVersionRow(String id, String planId, int versionNo, String status, String goal,
                          Instant profileUpdatedAt, String screeningId, int screeningVersion,
                          String assessmentAttemptId, String formulaVersion,
                          String targetPolicyVersion, BigDecimal bmi, int bmrKcalPerDay,
                          int tdeeKcalPerDay, int energyMinKcalPerDay, int energyMaxKcalPerDay,
                          BigDecimal weeklyWeightChangeMinPercent,
                          BigDecimal weeklyWeightChangeMaxPercent, Instant createdAt,
                          Instant validatedAt, Instant confirmedAt, Instant activatedAt,
                          Instant replacedAt) { }

    record DailyMetricRow(String id, LocalDate localDate, BigDecimal weightKg, Integer steps,
                          Integer activityMinutes, Integer sleepMinutes, Integer sleepQuality,
                          Instant createdAt) { }

    record NutritionRow(String id, LocalDate localDate, int energyKcal, BigDecimal proteinG,
                        BigDecimal carbohydrateG, BigDecimal fatG, Instant createdAt) { }

    record TrainingRow(String id, LocalDate localDate, String trainingType,
                       int durationMinutes, String intensity, Instant createdAt) { }

    record WeeklyReviewRow(String id, String planVersionId, LocalDate windowStart,
                           LocalDate windowEnd, int versionNo, String policyVersion,
                           int weightObservationDays, int nutritionLoggedDays,
                           int stepsObservedDays, int sleepObservedDays, int trainingDays,
                           BigDecimal averageWeightKg, BigDecimal weightTrendPercent,
                           Integer nutritionAdherencePercent, Integer averageSteps,
                           Integer averageSleepMinutes, int totalTrainingMinutes,
                           String recommendation, int proposedEnergyDeltaKcal, String reason,
                           Instant createdAt) { }

    record ConversationRow(String id, Instant createdAt, Instant updatedAt) { }

    record MessageRow(long id, String conversationId, int sequenceNo, String messageJson,
                      Instant createdAt) { }
}

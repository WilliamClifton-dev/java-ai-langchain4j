package com.atguigu.java.ai.langchain4j.tracking;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Mapper
interface WeeklyReviewMapper {
    @Select("SELECT id FROM user_account WHERE id = #{userId} FOR UPDATE")
    String lockUser(String userId);

    @Select("""
            SELECT local_date, weight_kg, steps, sleep_minutes
            FROM daily_metric
            WHERE user_id = #{userId} AND local_date BETWEEN #{start} AND #{end}
            ORDER BY local_date, id
            """)
    List<WeeklyMetricFact> metrics(@Param("userId") String userId,
                                   @Param("start") LocalDate start,
                                   @Param("end") LocalDate end);

    @Select("""
            SELECT local_date, energy_kcal
            FROM nutrition_log
            WHERE user_id = #{userId} AND local_date BETWEEN #{start} AND #{end}
            ORDER BY local_date, id
            """)
    List<WeeklyNutritionFact> nutrition(@Param("userId") String userId,
                                        @Param("start") LocalDate start,
                                        @Param("end") LocalDate end);

    @Select("""
            SELECT local_date, duration_minutes
            FROM training_log
            WHERE user_id = #{userId} AND local_date BETWEEN #{start} AND #{end}
            ORDER BY local_date, created_at, id
            """)
    List<WeeklyTrainingFact> training(@Param("userId") String userId,
                                      @Param("start") LocalDate start,
                                      @Param("end") LocalDate end);

    @Select("""
            SELECT COALESCE(MAX(version_no), 0) + 1
            FROM weekly_review
            WHERE user_id = #{userId} AND window_end = #{windowEnd}
            """)
    int nextVersion(@Param("userId") String userId, @Param("windowEnd") LocalDate windowEnd);

    @Select("""
            SELECT id, plan_version_id, window_start, window_end, version_no, policy_version,
                   weight_observation_days, nutrition_logged_days, steps_observed_days,
                   sleep_observed_days, training_days, average_weight_kg, weight_trend_percent,
                   nutrition_adherence_percent, average_steps, average_sleep_minutes,
                   total_training_minutes, recommendation, proposed_energy_delta_kcal,
                   reason, created_at
            FROM weekly_review
            WHERE user_id = #{userId} AND window_end = #{windowEnd} AND input_hash = #{inputHash}
            """)
    Optional<WeeklyReview> findByInput(@Param("userId") String userId,
                                       @Param("windowEnd") LocalDate windowEnd,
                                       @Param("inputHash") String inputHash);

    @Select("""
            SELECT id, plan_version_id, window_start, window_end, version_no, policy_version,
                   weight_observation_days, nutrition_logged_days, steps_observed_days,
                   sleep_observed_days, training_days, average_weight_kg, weight_trend_percent,
                   nutrition_adherence_percent, average_steps, average_sleep_minutes,
                   total_training_minutes, recommendation, proposed_energy_delta_kcal,
                   reason, created_at
            FROM weekly_review
            WHERE user_id = #{userId} AND id = #{reviewId}
            """)
    Optional<WeeklyReview> findById(@Param("userId") String userId,
                                    @Param("reviewId") String reviewId);

    @Insert("""
            INSERT INTO weekly_review (
              id, user_id, plan_version_id, window_start, window_end, version_no,
              input_hash, policy_version, weight_observation_days, nutrition_logged_days,
              steps_observed_days, sleep_observed_days, training_days, average_weight_kg,
              weight_trend_percent, nutrition_adherence_percent, average_steps,
              average_sleep_minutes, total_training_minutes, recommendation,
              proposed_energy_delta_kcal, reason, created_at
            ) VALUES (
              #{review.id}, #{userId}, #{review.planVersionId}, #{review.windowStart},
              #{review.windowEnd}, #{review.versionNo}, #{inputHash}, #{review.policyVersion},
              #{review.weightObservationDays}, #{review.nutritionLoggedDays},
              #{review.stepsObservedDays}, #{review.sleepObservedDays}, #{review.trainingDays},
              #{review.averageWeightKg}, #{review.weightTrendPercent},
              #{review.nutritionAdherencePercent}, #{review.averageSteps},
              #{review.averageSleepMinutes}, #{review.totalTrainingMinutes},
              #{review.recommendation}, #{review.proposedEnergyDeltaKcalPerDay},
              #{review.reason}, #{review.createdAt}
            )
            """)
    int insert(@Param("userId") String userId, @Param("inputHash") String inputHash,
               @Param("review") WeeklyReview review);
}

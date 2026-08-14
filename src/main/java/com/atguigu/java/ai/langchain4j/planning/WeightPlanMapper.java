package com.atguigu.java.ai.langchain4j.planning;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.Instant;
import java.util.Optional;

@Mapper
interface WeightPlanMapper {

    @Select("SELECT id FROM user_account WHERE id = #{userId} FOR UPDATE")
    String lockUser(String userId);

    @Select("""
            SELECT id, user_id, active_version_id, next_version_no
            FROM weight_plan
            WHERE user_id = #{userId}
            FOR UPDATE
            """)
    Optional<WeightPlanRow> findPlanForUpdate(String userId);

    @Insert("""
            INSERT INTO weight_plan (
                id, user_id, active_version_id, next_version_no, created_at, updated_at
            ) VALUES (#{id}, #{userId}, NULL, 2, #{now}, #{now})
            """)
    int insertPlan(
            @Param("id") String id,
            @Param("userId") String userId,
            @Param("now") Instant now
    );

    @Update("""
            UPDATE weight_plan
            SET next_version_no = #{nextVersionNo}, updated_at = #{now}
            WHERE id = #{planId} AND user_id = #{userId}
            """)
    int advanceVersion(
            @Param("planId") String planId,
            @Param("userId") String userId,
            @Param("nextVersionNo") int nextVersionNo,
            @Param("now") Instant now
    );

    @Insert("""
            INSERT INTO weight_plan_version (
                id, plan_id, version_no, draft_idempotency_key_hash, status, goal, profile_updated_at,
                screening_id, screening_version, assessment_attempt_id,
                formula_version, target_policy_version, bmi, bmr_kcal_per_day,
                tdee_kcal_per_day, energy_min_kcal_per_day, energy_max_kcal_per_day,
                weekly_weight_change_min_percent, weekly_weight_change_max_percent,
                created_at
            ) VALUES (
                #{version.id}, #{version.planId}, #{version.versionNo}, #{draftKeyHash},
                #{version.status}, #{version.goal}, #{version.profileUpdatedAt},
                #{version.screeningId}, #{version.screeningVersion}, #{version.assessmentAttemptId},
                #{version.formulaVersion}, #{version.targetPolicyVersion}, #{version.bmi},
                #{version.bmrKcalPerDay}, #{version.tdeeKcalPerDay},
                #{version.energyMinKcalPerDay}, #{version.energyMaxKcalPerDay},
                #{version.weeklyWeightChangeMinPercent},
                #{version.weeklyWeightChangeMaxPercent}, #{version.createdAt}
            )
            """)
    int insertVersion(
            @Param("version") WeightPlanVersion version,
            @Param("draftKeyHash") String draftKeyHash
    );

    @Select("""
            SELECT version.id, version.plan_id, version.version_no, version.status, version.goal,
                   version.profile_updated_at, version.screening_id, version.screening_version,
                   version.assessment_attempt_id, version.formula_version,
                   version.target_policy_version, version.bmi, version.bmr_kcal_per_day,
                   version.tdee_kcal_per_day, version.energy_min_kcal_per_day,
                   version.energy_max_kcal_per_day,
                   version.weekly_weight_change_min_percent,
                   version.weekly_weight_change_max_percent, version.created_at,
                   version.validated_at, version.confirmed_at, version.activated_at,
                   version.replaced_at
            FROM weight_plan_version version
            JOIN weight_plan plan ON plan.id = version.plan_id
            WHERE plan.user_id = #{userId}
              AND version.draft_idempotency_key_hash = #{keyHash}
            """)
    Optional<WeightPlanVersion> findByDraftKey(
            @Param("userId") String userId,
            @Param("keyHash") String keyHash
    );

    @Select("""
            SELECT version.id, version.plan_id, version.version_no, version.status, version.goal,
                   version.profile_updated_at, version.screening_id, version.screening_version,
                   version.assessment_attempt_id, version.formula_version,
                   version.target_policy_version, version.bmi, version.bmr_kcal_per_day,
                   version.tdee_kcal_per_day, version.energy_min_kcal_per_day,
                   version.energy_max_kcal_per_day,
                   version.weekly_weight_change_min_percent,
                   version.weekly_weight_change_max_percent, version.created_at,
                   version.validated_at, version.confirmed_at, version.activated_at,
                   version.replaced_at
            FROM weight_plan_version version
            JOIN weight_plan plan ON plan.id = version.plan_id
            WHERE plan.user_id = #{userId}
              AND version.activation_idempotency_key_hash = #{keyHash}
            """)
    Optional<WeightPlanVersion> findByActivationKey(
            @Param("userId") String userId,
            @Param("keyHash") String keyHash
    );

    @Select("""
            SELECT version.id, version.plan_id, version.version_no, version.status, version.goal,
                   version.profile_updated_at, version.screening_id, version.screening_version,
                   version.assessment_attempt_id, version.formula_version,
                   version.target_policy_version, version.bmi, version.bmr_kcal_per_day,
                   version.tdee_kcal_per_day, version.energy_min_kcal_per_day,
                   version.energy_max_kcal_per_day,
                   version.weekly_weight_change_min_percent,
                   version.weekly_weight_change_max_percent, version.created_at,
                   version.validated_at, version.confirmed_at, version.activated_at,
                   version.replaced_at
            FROM weight_plan_version version
            JOIN weight_plan plan ON plan.id = version.plan_id
            WHERE plan.user_id = #{userId}
              AND plan.id = #{planId}
              AND version.id = #{versionId}
            """)
    Optional<WeightPlanVersion> findVersion(
            @Param("userId") String userId,
            @Param("planId") String planId,
            @Param("versionId") String versionId
    );

    @Select("""
            SELECT version.id, version.plan_id, version.version_no, version.status, version.goal,
                   version.profile_updated_at, version.screening_id, version.screening_version,
                   version.assessment_attempt_id, version.formula_version,
                   version.target_policy_version, version.bmi, version.bmr_kcal_per_day,
                   version.tdee_kcal_per_day, version.energy_min_kcal_per_day,
                   version.energy_max_kcal_per_day,
                   version.weekly_weight_change_min_percent,
                   version.weekly_weight_change_max_percent, version.created_at,
                   version.validated_at, version.confirmed_at, version.activated_at,
                   version.replaced_at
            FROM weight_plan plan
            JOIN weight_plan_version version ON version.id = plan.active_version_id
            WHERE plan.user_id = #{userId}
            """)
    Optional<WeightPlanVersion> findActive(String userId);

    @Update("""
            UPDATE weight_plan_version
            SET status = #{nextStatus},
                validated_at = COALESCE(#{validatedAt}, validated_at),
                confirmed_at = COALESCE(#{confirmedAt}, confirmed_at),
                activated_at = COALESCE(#{activatedAt}, activated_at),
                replaced_at = COALESCE(#{replacedAt}, replaced_at),
                activation_idempotency_key_hash =
                    COALESCE(#{activationKeyHash}, activation_idempotency_key_hash)
            WHERE id = #{versionId} AND plan_id = #{planId} AND status = #{expectedStatus}
              AND EXISTS (
                  SELECT 1 FROM weight_plan plan
                  WHERE plan.id = weight_plan_version.plan_id
                    AND plan.user_id = #{userId}
              )
            """)
    int transition(
            @Param("userId") String userId,
            @Param("planId") String planId,
            @Param("versionId") String versionId,
            @Param("expectedStatus") PlanVersionStatus expectedStatus,
            @Param("nextStatus") PlanVersionStatus nextStatus,
            @Param("validatedAt") Instant validatedAt,
            @Param("confirmedAt") Instant confirmedAt,
            @Param("activatedAt") Instant activatedAt,
            @Param("replacedAt") Instant replacedAt,
            @Param("activationKeyHash") String activationKeyHash
    );

    @Update("""
            UPDATE weight_plan
            SET active_version_id = #{versionId}, updated_at = #{now}
            WHERE id = #{planId} AND user_id = #{userId}
            """)
    int setActiveVersion(
            @Param("planId") String planId,
            @Param("userId") String userId,
            @Param("versionId") String versionId,
            @Param("now") Instant now
    );
}

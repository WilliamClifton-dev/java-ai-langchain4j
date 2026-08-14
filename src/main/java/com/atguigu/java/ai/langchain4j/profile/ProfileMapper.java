package com.atguigu.java.ai.langchain4j.profile;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.Instant;
import java.util.Optional;

@Mapper
public interface ProfileMapper {

    @Insert("""
            INSERT INTO user_profile (
                user_id, date_of_birth, calculation_sex, height_cm,
                current_weight_kg, target_weight_kg, activity_level, time_zone,
                screening_version, created_at, updated_at
            ) VALUES (
                #{userId}, #{dateOfBirth}, #{calculationSex}, #{heightCm},
                #{currentWeightKg}, #{targetWeightKg}, #{activityLevel}, #{timeZone},
                #{screeningVersion}, #{createdAt}, #{updatedAt}
            )
            ON DUPLICATE KEY UPDATE
                date_of_birth = VALUES(date_of_birth),
                calculation_sex = VALUES(calculation_sex),
                height_cm = VALUES(height_cm),
                current_weight_kg = VALUES(current_weight_kg),
                target_weight_kg = VALUES(target_weight_kg),
                activity_level = VALUES(activity_level),
                time_zone = VALUES(time_zone),
                updated_at = VALUES(updated_at)
            """)
    int upsert(UserProfile profile);

    @Select("""
            SELECT user_id, date_of_birth, calculation_sex, height_cm,
                   current_weight_kg, target_weight_kg, activity_level, time_zone,
                   screening_version, created_at, updated_at
            FROM user_profile
            WHERE user_id = #{userId}
            """)
    Optional<UserProfile> findByUserId(String userId);

    @Select("""
            SELECT user_id, date_of_birth, calculation_sex, height_cm,
                   current_weight_kg, target_weight_kg, activity_level, time_zone,
                   screening_version, created_at, updated_at
            FROM user_profile
            WHERE user_id = #{userId}
            FOR UPDATE
            """)
    Optional<UserProfile> findByUserIdForUpdate(String userId);

    @Update("""
            UPDATE user_profile
            SET screening_version = #{version}, updated_at = #{updatedAt}
            WHERE user_id = #{userId} AND screening_version = #{previousVersion}
            """)
    int advanceScreeningVersion(
            @Param("userId") String userId,
            @Param("previousVersion") int previousVersion,
            @Param("version") int version,
            @Param("updatedAt") Instant updatedAt
    );

    @Insert("""
            INSERT INTO safety_screening (
                id, user_id, version, pregnant_or_breastfeeding,
                eating_disorder_history, medical_guidance_required,
                weight_affecting_medication, concerning_symptoms,
                status, automatic_planning_allowed, reason_codes, created_at
            ) VALUES (
                #{id}, #{userId}, #{version}, #{pregnantOrBreastfeeding},
                #{eatingDisorderHistory}, #{medicalGuidanceRequired},
                #{weightAffectingMedication}, #{concerningSymptoms},
                #{status}, #{automaticPlanningAllowed}, #{reasonCodes}, #{createdAt}
            )
            """)
    int insertScreening(SafetyScreening screening);

    @Select("""
            SELECT id, user_id, version, pregnant_or_breastfeeding,
                   eating_disorder_history, medical_guidance_required,
                   weight_affecting_medication, concerning_symptoms,
                   status, automatic_planning_allowed, reason_codes, created_at
            FROM safety_screening
            WHERE user_id = #{userId}
            ORDER BY version DESC
            LIMIT 1
            """)
    Optional<SafetyScreening> findCurrentScreening(String userId);
}

package com.atguigu.java.ai.langchain4j.tracking;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Mapper
interface DailyTrackingMapper {
    @Select("SELECT id FROM user_account WHERE id = #{userId} FOR UPDATE")
    String lockUser(String userId);

    @Insert("""
            INSERT INTO daily_metric (id, user_id, local_date, idempotency_key_hash, payload_hash,
              weight_kg, steps, activity_minutes, sleep_minutes, sleep_quality, created_at)
            VALUES (#{record.id}, #{userId}, #{record.localDate}, #{keyHash}, #{payloadHash},
              #{record.weightKg}, #{record.steps}, #{record.activityMinutes},
              #{record.sleepMinutes}, #{record.sleepQuality}, #{record.createdAt})
            """)
    int insertMetric(@Param("userId") String userId, @Param("keyHash") String keyHash,
                     @Param("payloadHash") String payloadHash, @Param("record") DailyMetric record);

    @Select("""
            SELECT id, local_date, weight_kg, steps, activity_minutes, sleep_minutes,
                   sleep_quality, created_at, payload_hash
            FROM daily_metric WHERE user_id = #{userId} AND idempotency_key_hash = #{keyHash}
            """)
    Optional<DailyMetricRow> metricByKey(@Param("userId") String userId, @Param("keyHash") String keyHash);

    @Select("""
            SELECT id, local_date, weight_kg, steps, activity_minutes, sleep_minutes,
                   sleep_quality, created_at, payload_hash
            FROM daily_metric WHERE user_id = #{userId} AND local_date = #{date}
            """)
    Optional<DailyMetricRow> metricByDate(@Param("userId") String userId, @Param("date") LocalDate date);

    @Insert("""
            INSERT INTO nutrition_log (id, user_id, local_date, idempotency_key_hash, payload_hash,
              energy_kcal, protein_g, carbohydrate_g, fat_g, created_at)
            VALUES (#{record.id}, #{userId}, #{record.localDate}, #{keyHash}, #{payloadHash},
              #{record.energyKcal}, #{record.proteinG}, #{record.carbohydrateG},
              #{record.fatG}, #{record.createdAt})
            """)
    int insertNutrition(@Param("userId") String userId, @Param("keyHash") String keyHash,
                        @Param("payloadHash") String payloadHash, @Param("record") NutritionLog record);

    @Select("""
            SELECT id, local_date, energy_kcal, protein_g, carbohydrate_g, fat_g,
                   created_at, payload_hash
            FROM nutrition_log WHERE user_id = #{userId} AND idempotency_key_hash = #{keyHash}
            """)
    Optional<NutritionRow> nutritionByKey(@Param("userId") String userId, @Param("keyHash") String keyHash);

    @Select("""
            SELECT id, local_date, energy_kcal, protein_g, carbohydrate_g, fat_g,
                   created_at, payload_hash
            FROM nutrition_log WHERE user_id = #{userId} AND local_date = #{date}
            """)
    Optional<NutritionRow> nutritionByDate(@Param("userId") String userId, @Param("date") LocalDate date);

    @Insert("""
            INSERT INTO training_log (id, user_id, local_date, idempotency_key_hash, payload_hash,
              training_type, duration_minutes, intensity, created_at)
            VALUES (#{record.id}, #{userId}, #{record.localDate}, #{keyHash}, #{payloadHash},
              #{record.trainingType}, #{record.durationMinutes}, #{record.intensity}, #{record.createdAt})
            """)
    int insertTraining(@Param("userId") String userId, @Param("keyHash") String keyHash,
                       @Param("payloadHash") String payloadHash, @Param("record") TrainingLog record);

    @Select("""
            SELECT id, local_date, training_type, duration_minutes, intensity,
                   created_at, payload_hash
            FROM training_log WHERE user_id = #{userId} AND idempotency_key_hash = #{keyHash}
            """)
    Optional<TrainingRow> trainingByKey(@Param("userId") String userId, @Param("keyHash") String keyHash);

    @Select("""
            SELECT id, local_date, training_type, duration_minutes, intensity,
                   created_at, payload_hash
            FROM training_log WHERE user_id = #{userId} AND local_date = #{date}
            ORDER BY created_at, id
            """)
    List<TrainingRow> trainingByDate(@Param("userId") String userId, @Param("date") LocalDate date);
}

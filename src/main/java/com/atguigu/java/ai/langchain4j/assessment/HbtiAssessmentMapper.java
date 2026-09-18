package com.atguigu.java.ai.langchain4j.assessment;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Mapper
interface HbtiAssessmentMapper {

    @Select("SELECT id FROM user_account WHERE id = #{userId} FOR UPDATE")
    String lockUser(String userId);

    @Select("""
            SELECT attempt.id, definition.version AS definition_version,
                   definition.scoring_rule_version, attempt.payload_hash,
                   attempt.type_code, attempt.completed_at
            FROM assessment_attempt attempt
            JOIN assessment_definition definition ON definition.id = attempt.definition_id
            WHERE attempt.user_id = #{userId}
              AND attempt.idempotency_key_hash = #{idempotencyKeyHash}
            """)
    Optional<HbtiAssessmentResultRow> findByIdempotencyKey(
            @Param("userId") String userId,
            @Param("idempotencyKeyHash") String idempotencyKeyHash
    );

    @Insert("""
            INSERT INTO assessment_attempt (
                id, user_id, definition_id, idempotency_key_hash, payload_hash,
                status, type_code, created_at, completed_at
            ) VALUES (
                #{id}, #{userId}, #{definitionId}, #{idempotencyKeyHash}, #{payloadHash},
                'COMPLETED', #{typeCode}, #{completedAt}, #{completedAt}
            )
            """)
    int insertAttempt(
            @Param("id") String id,
            @Param("userId") String userId,
            @Param("definitionId") String definitionId,
            @Param("idempotencyKeyHash") String idempotencyKeyHash,
            @Param("payloadHash") String payloadHash,
            @Param("typeCode") String typeCode,
            @Param("completedAt") Instant completedAt
    );

    @Insert("""
            INSERT INTO assessment_answer (attempt_id, item_id, item_key, answer_value)
            VALUES (#{attemptId}, #{itemId}, #{itemKey}, #{value})
            """)
    int insertAnswer(
            @Param("attemptId") String attemptId,
            @Param("itemId") String itemId,
            @Param("itemKey") String itemKey,
            @Param("value") int value
    );

    @Insert("""
            INSERT INTO assessment_score (
                attempt_id, dimension_code, ordinal, chosen_pole, left_score, right_score
            ) VALUES (
                #{attemptId}, #{score.dimensionCode}, #{ordinal}, #{score.chosenPole},
                #{score.leftScore}, #{score.rightScore}
            )
            """)
    int insertScore(
            @Param("attemptId") String attemptId,
            @Param("ordinal") int ordinal,
            @Param("score") HbtiDimensionScore score
    );

    @Select("""
            SELECT attempt.id, definition.version AS definition_version,
                   definition.scoring_rule_version, attempt.payload_hash,
                   attempt.type_code, attempt.completed_at
            FROM assessment_attempt attempt
            JOIN assessment_definition definition ON definition.id = attempt.definition_id
            WHERE attempt.user_id = #{userId}
            ORDER BY attempt.completed_at DESC, attempt.id DESC
            LIMIT 1
            """)
    Optional<HbtiAssessmentResultRow> findCurrent(String userId);

    @Select("""
            SELECT attempt.id, definition.version AS definition_version,
                   definition.scoring_rule_version, attempt.payload_hash,
                   attempt.type_code, attempt.completed_at
            FROM assessment_attempt attempt
            JOIN assessment_definition definition ON definition.id = attempt.definition_id
            WHERE attempt.user_id = #{userId}
            ORDER BY attempt.completed_at DESC, attempt.id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<HbtiAssessmentResultRow> findHistory(
            @Param("userId") String userId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Select("SELECT COUNT(*) FROM assessment_attempt WHERE user_id = #{userId}")
    long countHistory(String userId);

    @Select("""
            SELECT dimension_code, chosen_pole, left_score, right_score
            FROM assessment_score
            WHERE attempt_id = #{attemptId}
            ORDER BY ordinal
            """)
    List<HbtiDimensionScore> findScores(String attemptId);
}

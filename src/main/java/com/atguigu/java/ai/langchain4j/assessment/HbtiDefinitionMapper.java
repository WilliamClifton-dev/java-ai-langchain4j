package com.atguigu.java.ai.langchain4j.assessment;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
interface HbtiDefinitionMapper {

    @Select("""
            SELECT id, assessment_key, version, scoring_rule_version, display_name,
                   status, answer_min, answer_max, source_repository, source_commit,
                   source_content_hash, published_at
            FROM assessment_definition
            WHERE assessment_key = #{assessmentKey}
              AND version = #{version}
              AND status = 'PUBLISHED'
            """)
    Optional<HbtiDefinitionMetadata> findPublishedMetadata(
            @Param("assessmentKey") String assessmentKey,
            @Param("version") String version
    );

    @Select("""
            SELECT definition_id, dimension_code AS code, ordinal,
                   left_pole, right_pole, left_label, right_label,
                   description_zh, description_en
            FROM assessment_dimension
            WHERE definition_id = #{definitionId}
            ORDER BY ordinal
            """)
    List<HbtiDimensionDefinition> findDimensions(String definitionId);

    @Select("""
            SELECT id, definition_id, item_key, ordinal, dimension_code,
                   target_pole, title_zh, hint_zh, title_en, hint_en
            FROM assessment_item
            WHERE definition_id = #{definitionId}
            ORDER BY ordinal
            """)
    List<HbtiItemDefinition> findItems(String definitionId);
}

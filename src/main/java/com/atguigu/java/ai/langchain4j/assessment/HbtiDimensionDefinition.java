package com.atguigu.java.ai.langchain4j.assessment;

public record HbtiDimensionDefinition(
        String definitionId,
        String code,
        int ordinal,
        String leftPole,
        String rightPole,
        String leftLabel,
        String rightLabel,
        String descriptionZh,
        String descriptionEn
) {
}

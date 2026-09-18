package com.atguigu.java.ai.langchain4j.assessment;

public record HbtiItemDefinition(
        String id,
        String definitionId,
        String itemKey,
        int ordinal,
        String dimensionCode,
        String targetPole,
        String titleZh,
        String hintZh,
        String titleEn,
        String hintEn
) {
}

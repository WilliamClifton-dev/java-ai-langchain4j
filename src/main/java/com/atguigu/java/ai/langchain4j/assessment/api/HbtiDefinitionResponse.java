package com.atguigu.java.ai.langchain4j.assessment.api;

import com.atguigu.java.ai.langchain4j.assessment.HbtiDefinition;
import com.atguigu.java.ai.langchain4j.assessment.HbtiDimensionDefinition;
import com.atguigu.java.ai.langchain4j.assessment.HbtiItemDefinition;

import java.util.List;

public record HbtiDefinitionResponse(
        String version,
        String displayName,
        int answerMin,
        int answerMax,
        List<Dimension> dimensions,
        List<Item> items,
        String limitation
) {
    private static final String LIMITATION =
            "HBTI is an exploratory behavioral tendency assessment, not a diagnosis.";

    public HbtiDefinitionResponse {
        dimensions = List.copyOf(dimensions);
        items = List.copyOf(items);
    }

    static HbtiDefinitionResponse from(HbtiDefinition definition) {
        return new HbtiDefinitionResponse(
                definition.version(),
                definition.displayName(),
                definition.answerMin(),
                definition.answerMax(),
                definition.dimensions().stream().map(Dimension::from).toList(),
                definition.items().stream().map(Item::from).toList(),
                LIMITATION
        );
    }

    public record Dimension(
            String code,
            int ordinal,
            String leftPole,
            String rightPole,
            String leftLabel,
            String rightLabel,
            String descriptionZh,
            String descriptionEn
    ) {
        static Dimension from(HbtiDimensionDefinition definition) {
            return new Dimension(
                    definition.code(), definition.ordinal(),
                    definition.leftPole(), definition.rightPole(),
                    definition.leftLabel(), definition.rightLabel(),
                    definition.descriptionZh(), definition.descriptionEn()
            );
        }
    }

    public record Item(
            String itemKey,
            int ordinal,
            String titleZh,
            String hintZh,
            String titleEn,
            String hintEn
    ) {
        static Item from(HbtiItemDefinition definition) {
            return new Item(
                    definition.itemKey(), definition.ordinal(),
                    definition.titleZh(), definition.hintZh(),
                    definition.titleEn(), definition.hintEn()
            );
        }
    }
}

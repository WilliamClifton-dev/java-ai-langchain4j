package com.atguigu.java.ai.langchain4j.assessment;

public record HbtiDimensionScore(
        String dimensionCode,
        String chosenPole,
        int leftScore,
        int rightScore
) {
}

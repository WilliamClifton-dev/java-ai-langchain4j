package com.atguigu.java.ai.langchain4j.planning;

record WeightPlanRow(
        String id,
        String userId,
        String activeVersionId,
        int nextVersionNo
) {
}

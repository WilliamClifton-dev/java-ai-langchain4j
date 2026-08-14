package com.atguigu.java.ai.langchain4j.assessment;

import java.util.List;

public record HbtiAssessmentPage(
        List<HbtiAssessmentResult> items,
        int page,
        int pageSize,
        long totalItems
) {
    public HbtiAssessmentPage {
        items = List.copyOf(items);
    }
}

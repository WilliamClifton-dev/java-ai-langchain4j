package com.atguigu.java.ai.langchain4j.assessment.api;

import com.atguigu.java.ai.langchain4j.assessment.HbtiAssessmentPage;

import java.util.List;

public record HbtiAssessmentPageResponse(
        List<HbtiAssessmentResultResponse> items,
        int page,
        int pageSize,
        long totalItems
) {
    static HbtiAssessmentPageResponse from(HbtiAssessmentPage resultPage) {
        return new HbtiAssessmentPageResponse(
                resultPage.items().stream().map(HbtiAssessmentResultResponse::from).toList(),
                resultPage.page(), resultPage.pageSize(), resultPage.totalItems()
        );
    }
}

package com.atguigu.java.ai.langchain4j.tracking.api;

import com.atguigu.java.ai.langchain4j.tracking.WeeklyReviewWrite;

public record WeeklyReviewWriteResponse(WeeklyReviewResponse review, boolean replayed) {
    static WeeklyReviewWriteResponse from(WeeklyReviewWrite value) {
        return new WeeklyReviewWriteResponse(
                WeeklyReviewResponse.from(value.review()), value.replayed()
        );
    }
}

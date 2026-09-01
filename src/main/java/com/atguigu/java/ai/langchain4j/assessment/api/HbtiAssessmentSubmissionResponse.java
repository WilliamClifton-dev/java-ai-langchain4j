package com.atguigu.java.ai.langchain4j.assessment.api;

import com.atguigu.java.ai.langchain4j.assessment.HbtiAssessmentSubmission;

public record HbtiAssessmentSubmissionResponse(
        HbtiAssessmentResultResponse result,
        boolean replayed
) {
    static HbtiAssessmentSubmissionResponse from(HbtiAssessmentSubmission submission) {
        return new HbtiAssessmentSubmissionResponse(
                HbtiAssessmentResultResponse.from(submission.result()),
                submission.replayed()
        );
    }
}

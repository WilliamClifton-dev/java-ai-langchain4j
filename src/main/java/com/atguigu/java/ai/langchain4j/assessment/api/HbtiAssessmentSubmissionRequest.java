package com.atguigu.java.ai.langchain4j.assessment.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record HbtiAssessmentSubmissionRequest(
        @NotBlank @Size(max = 32) String definitionVersion,
        @NotEmpty @Size(max = 64) List<@Valid HbtiAnswerRequest> answers
) {
}

package com.atguigu.java.ai.langchain4j.assessment.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HbtiAnswerRequest(
        @NotBlank @Size(max = 16) String itemKey,
        @NotNull @Min(1) @Max(5) Integer value
) {
}

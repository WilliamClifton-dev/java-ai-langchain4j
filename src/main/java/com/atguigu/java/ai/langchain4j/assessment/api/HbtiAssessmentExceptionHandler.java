package com.atguigu.java.ai.langchain4j.assessment.api;

import com.atguigu.java.ai.langchain4j.assessment.AssessmentDefinitionNotFoundException;
import com.atguigu.java.ai.langchain4j.assessment.AssessmentResultNotFoundException;
import com.atguigu.java.ai.langchain4j.assessment.IdempotencyConflictException;
import com.atguigu.java.ai.langchain4j.assessment.InvalidAssessmentAnswersException;
import com.atguigu.java.ai.langchain4j.assessment.InvalidAssessmentRequestException;
import com.atguigu.java.ai.langchain4j.common.api.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class HbtiAssessmentExceptionHandler {

    @ExceptionHandler(InvalidAssessmentAnswersException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse invalidAnswers() {
        return error("INVALID_ASSESSMENT_ANSWERS", "A complete valid answer set is required");
    }

    @ExceptionHandler(InvalidAssessmentRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse invalidRequest() {
        return error("INVALID_ASSESSMENT_REQUEST", "Assessment request is invalid");
    }

    @ExceptionHandler(AssessmentDefinitionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse definitionNotFound() {
        return error("ASSESSMENT_DEFINITION_NOT_FOUND", "Published assessment definition was not found");
    }

    @ExceptionHandler(AssessmentResultNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse resultNotFound() {
        return error("ASSESSMENT_RESULT_NOT_FOUND", "Assessment result was not found");
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse idempotencyConflict() {
        return error("IDEMPOTENCY_CONFLICT", "Idempotency key was used with a different request");
    }

    private ApiErrorResponse error(String code, String message) {
        return new ApiErrorResponse(new ApiErrorResponse.ApiError(code, message, Map.of()));
    }
}

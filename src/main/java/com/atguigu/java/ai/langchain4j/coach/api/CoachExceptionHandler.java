package com.atguigu.java.ai.langchain4j.coach.api;

import com.atguigu.java.ai.langchain4j.coach.service.CoachModelException;
import com.atguigu.java.ai.langchain4j.common.api.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = CoachController.class)
public class CoachExceptionHandler {
    @ExceptionHandler(CoachModelException.class)
    public ResponseEntity<ApiErrorResponse> modelFailure(CoachModelException exception) {
        HttpStatus status = switch (exception.code()) {
            case "MODEL_RATE_LIMITED", "MODEL_CONCURRENCY_LIMIT" -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.SERVICE_UNAVAILABLE;
        };
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                new ApiErrorResponse.ApiError(exception.code(), "Coach model is temporarily unavailable", Map.of())));
    }
}

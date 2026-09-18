package com.atguigu.java.ai.langchain4j.planning.api;

import com.atguigu.java.ai.langchain4j.common.api.ApiErrorResponse;
import com.atguigu.java.ai.langchain4j.planning.InvalidPlanTransitionException;
import com.atguigu.java.ai.langchain4j.planning.InvalidPlanRequestException;
import com.atguigu.java.ai.langchain4j.planning.PlanIdempotencyConflictException;
import com.atguigu.java.ai.langchain4j.planning.PlanVersionNotFoundException;
import com.atguigu.java.ai.langchain4j.planning.PlanningPrerequisiteException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class WeightPlanExceptionHandler {

    @ExceptionHandler(PlanVersionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse notFound() {
        return error("PLAN_VERSION_NOT_FOUND", "Plan version was not found", Map.of());
    }

    @ExceptionHandler(InvalidPlanTransitionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse invalidTransition() {
        return error(
                "INVALID_PLAN_TRANSITION",
                "Plan version is not in the required state",
                Map.of()
        );
    }

    @ExceptionHandler(PlanIdempotencyConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse idempotencyConflict() {
        return error(
                "PLAN_IDEMPOTENCY_CONFLICT",
                "Idempotency key was already used for another plan operation",
                Map.of()
        );
    }

    @ExceptionHandler(InvalidPlanRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse invalidRequest() {
        return error("INVALID_PLAN_REQUEST", "Plan request is invalid", Map.of());
    }

    @ExceptionHandler(PlanningPrerequisiteException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse prerequisites(PlanningPrerequisiteException exception) {
        return error(
                "PLANNING_PREREQUISITE_NOT_MET",
                "Planning prerequisites are missing, blocked, or stale",
                Map.of("reason", exception.reason().name())
        );
    }

    private ApiErrorResponse error(String code, String message, Map<String, String> details) {
        return new ApiErrorResponse(new ApiErrorResponse.ApiError(code, message, details));
    }
}

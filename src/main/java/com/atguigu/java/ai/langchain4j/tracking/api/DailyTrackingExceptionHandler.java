package com.atguigu.java.ai.langchain4j.tracking.api;

import com.atguigu.java.ai.langchain4j.common.api.ApiErrorResponse;
import com.atguigu.java.ai.langchain4j.tracking.InvalidTrackingRequestException;
import com.atguigu.java.ai.langchain4j.tracking.TrackingDateConflictException;
import com.atguigu.java.ai.langchain4j.tracking.TrackingIdempotencyConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class DailyTrackingExceptionHandler {
    @ExceptionHandler(InvalidTrackingRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse invalidRequest() {
        return error("INVALID_TRACKING_REQUEST", "Tracking request is invalid");
    }

    @ExceptionHandler(TrackingIdempotencyConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse idempotencyConflict() {
        return error("TRACKING_IDEMPOTENCY_CONFLICT",
                "Idempotency key was already used for another tracking record");
    }

    @ExceptionHandler(TrackingDateConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse dateConflict() {
        return error("TRACKING_DATE_CONFLICT", "A tracking record already exists for this date");
    }

    private ApiErrorResponse error(String code, String message) {
        return new ApiErrorResponse(new ApiErrorResponse.ApiError(code, message, Map.of()));
    }
}

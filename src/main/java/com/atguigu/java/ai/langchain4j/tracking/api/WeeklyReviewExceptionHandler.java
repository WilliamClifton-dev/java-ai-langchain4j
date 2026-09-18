package com.atguigu.java.ai.langchain4j.tracking.api;

import com.atguigu.java.ai.langchain4j.common.api.ApiErrorResponse;
import com.atguigu.java.ai.langchain4j.tracking.InvalidWeeklyReviewException;
import com.atguigu.java.ai.langchain4j.tracking.WeeklyReviewNotFoundException;
import com.atguigu.java.ai.langchain4j.tracking.WeeklyReviewPrerequisiteException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class WeeklyReviewExceptionHandler {
    @ExceptionHandler(InvalidWeeklyReviewException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse invalid() {
        return error("INVALID_WEEKLY_REVIEW_REQUEST", "Weekly review request is invalid");
    }

    @ExceptionHandler(WeeklyReviewPrerequisiteException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse prerequisite() {
        return error("WEEKLY_REVIEW_PREREQUISITE_NOT_MET",
                "A profile and active plan are required");
    }

    @ExceptionHandler(WeeklyReviewNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse notFound() {
        return error("WEEKLY_REVIEW_NOT_FOUND", "Weekly review was not found");
    }

    private ApiErrorResponse error(String code, String message) {
        return new ApiErrorResponse(new ApiErrorResponse.ApiError(code, message, Map.of()));
    }
}

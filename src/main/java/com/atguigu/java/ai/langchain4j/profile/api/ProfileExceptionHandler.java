package com.atguigu.java.ai.langchain4j.profile.api;

import com.atguigu.java.ai.langchain4j.common.api.ApiErrorResponse;
import com.atguigu.java.ai.langchain4j.profile.InvalidProfileException;
import com.atguigu.java.ai.langchain4j.profile.ProfileNotFoundException;
import com.atguigu.java.ai.langchain4j.profile.ProfileRequiredException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ProfileExceptionHandler {

    @ExceptionHandler(ProfileNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse notFound() {
        return error("PROFILE_NOT_FOUND", "Profile was not found");
    }

    @ExceptionHandler(ProfileRequiredException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse profileRequired() {
        return error("PROFILE_REQUIRED", "Complete a profile before safety screening");
    }

    @ExceptionHandler(InvalidProfileException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse invalidProfile() {
        return error("INVALID_PROFILE", "Profile values are outside supported bounds");
    }

    private ApiErrorResponse error(String code, String message) {
        return new ApiErrorResponse(new ApiErrorResponse.ApiError(code, message, Map.of()));
    }
}

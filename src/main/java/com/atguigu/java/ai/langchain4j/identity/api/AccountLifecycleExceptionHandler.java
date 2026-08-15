package com.atguigu.java.ai.langchain4j.identity.api;

import com.atguigu.java.ai.langchain4j.common.api.ApiErrorResponse;
import com.atguigu.java.ai.langchain4j.identity.AccountDataNotFoundException;
import com.atguigu.java.ai.langchain4j.identity.DataExportTooLargeException;
import com.atguigu.java.ai.langchain4j.identity.InvalidAccountDeletionConfirmationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class AccountLifecycleExceptionHandler {

    @ExceptionHandler(InvalidAccountDeletionConfirmationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse invalidConfirmation() {
        return error("INVALID_ACCOUNT_DELETION_CONFIRMATION",
                "Account deletion confirmation is invalid");
    }

    @ExceptionHandler(AccountDataNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse accountNotFound() {
        return error("ACCOUNT_NOT_FOUND", "Account data is unavailable");
    }

    @ExceptionHandler(DataExportTooLargeException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ApiErrorResponse exportTooLarge() {
        return error("ACCOUNT_EXPORT_TOO_LARGE", "Account data export exceeds the allowed limit");
    }

    private ApiErrorResponse error(String code, String message) {
        return new ApiErrorResponse(new ApiErrorResponse.ApiError(code, message, Map.of()));
    }
}

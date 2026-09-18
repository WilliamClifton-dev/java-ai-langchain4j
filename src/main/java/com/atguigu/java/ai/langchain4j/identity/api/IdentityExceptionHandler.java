package com.atguigu.java.ai.langchain4j.identity.api;

import com.atguigu.java.ai.langchain4j.common.api.ApiErrorResponse;
import com.atguigu.java.ai.langchain4j.identity.EmailAlreadyRegisteredException;
import com.atguigu.java.ai.langchain4j.identity.InvalidCredentialInputException;
import com.atguigu.java.ai.langchain4j.identity.InvalidCredentialsException;
import com.atguigu.java.ai.langchain4j.identity.InvalidRefreshTokenException;
import com.atguigu.java.ai.langchain4j.identity.RefreshTokenReuseException;
import com.atguigu.java.ai.langchain4j.identity.TooManyLoginAttemptsException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class IdentityExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleDuplicateEmail() {
        return error("EMAIL_ALREADY_REGISTERED", "An account already exists for this email");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleInvalidCredentials() {
        return error("INVALID_CREDENTIALS", "Email or password is incorrect");
    }

    @ExceptionHandler(InvalidCredentialInputException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleInvalidCredentialInput() {
        return error("INVALID_CREDENTIAL_INPUT", "Credentials do not meet the required format");
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleInvalidRefreshToken() {
        return error("INVALID_REFRESH_TOKEN", "Refresh token is invalid or expired");
    }

    @ExceptionHandler(RefreshTokenReuseException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleRefreshTokenReuse() {
        return error("SESSION_REVOKED", "Session has been revoked");
    }

    @ExceptionHandler(TooManyLoginAttemptsException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ApiErrorResponse handleTooManyLoginAttempts() {
        return error("LOGIN_RATE_LIMITED", "Too many login attempts; try again later");
    }

    private ApiErrorResponse error(String code, String message) {
        return new ApiErrorResponse(new ApiErrorResponse.ApiError(code, message, Map.of()));
    }
}

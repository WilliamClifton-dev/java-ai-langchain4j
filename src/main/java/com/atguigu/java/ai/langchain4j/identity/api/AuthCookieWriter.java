package com.atguigu.java.ai.langchain4j.identity.api;

import com.atguigu.java.ai.langchain4j.identity.AuthProperties;
import com.atguigu.java.ai.langchain4j.identity.AuthSession;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Clock;
import java.time.Instant;

@Component
class AuthCookieWriter {

    static final String ACCESS_COOKIE = "HBTI_ACCESS";
    static final String REFRESH_COOKIE = "HBTI_REFRESH";

    private final AuthProperties authProperties;
    private final Clock clock;

    AuthCookieWriter(AuthProperties authProperties, Clock clock) {
        this.authProperties = authProperties;
        this.clock = clock;
    }

    void writeSession(HttpServletResponse response, AuthSession session) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(
                ACCESS_COOKIE,
                session.accessToken().value(),
                "/",
                durationUntil(session.accessToken().expiresAt())
        ).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(
                REFRESH_COOKIE,
                session.refreshToken().value(),
                "/api/v1/auth",
                durationUntil(session.refreshToken().expiresAt())
        ).toString());
    }

    void clearSession(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(ACCESS_COOKIE, "", "/", Duration.ZERO).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(
                REFRESH_COOKIE, "", "/api/v1/auth", Duration.ZERO
        ).toString());
    }

    private ResponseCookie cookie(String name, String value, String path, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(authProperties.secureCookies())
                .sameSite("Strict")
                .path(path)
                .maxAge(maxAge)
                .build();
    }

    private Duration durationUntil(Instant expiry) {
        Duration duration = Duration.between(clock.instant(), expiry);
        return duration.isNegative() ? Duration.ZERO : duration;
    }
}

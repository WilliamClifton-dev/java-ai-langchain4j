package com.atguigu.java.ai.langchain4j.identity.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

import java.util.Arrays;

public class CookieBearerTokenResolver implements BearerTokenResolver {

    private static final String ACCESS_COOKIE = "HBTI_ACCESS";
    private final DefaultBearerTokenResolver headerResolver = new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest request) {
        String headerToken = headerResolver.resolve(request);
        String cookieToken = findCookie(request);
        if (headerToken != null && cookieToken != null && !headerToken.equals(cookieToken)) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    "invalid_request", "Multiple bearer tokens are not allowed", null
            ));
        }
        return headerToken != null ? headerToken : cookieToken;
    }

    private String findCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> ACCESS_COOKIE.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(null);
    }
}

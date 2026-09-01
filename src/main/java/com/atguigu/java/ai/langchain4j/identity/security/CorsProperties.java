package com.atguigu.java.ai.langchain4j.identity.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;

@ConfigurationProperties("hbti.security.cors")
public record CorsProperties(List<String> allowedOrigins, Duration maxAge) {
    private static final int MAX_ORIGINS = 10;
    private static final Duration MAX_MAX_AGE = Duration.ofDays(1);

    public CorsProperties {
        if (allowedOrigins == null || allowedOrigins.isEmpty()
                || allowedOrigins.size() > MAX_ORIGINS
                || new LinkedHashSet<>(allowedOrigins).size() != allowedOrigins.size()) {
            throw new IllegalArgumentException("hbti.security.cors.allowed-origins must be 1-10 unique origins");
        }
        for (String origin : allowedOrigins) validateOrigin(origin);
        if (maxAge == null || maxAge.isNegative() || maxAge.compareTo(MAX_MAX_AGE) > 0) {
            throw new IllegalArgumentException("hbti.security.cors.max-age must be between 0 and 1 day");
        }
        allowedOrigins = List.copyOf(allowedOrigins);
    }

    private void validateOrigin(String raw) {
        if (raw == null || raw.isBlank() || "*".equals(raw)) {
            throw new IllegalArgumentException("CORS origin must be explicit");
        }
        URI origin;
        try {
            origin = URI.create(raw);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("CORS origin is invalid", exception);
        }
        if (!("http".equalsIgnoreCase(origin.getScheme())
                || "https".equalsIgnoreCase(origin.getScheme()))
                || origin.getHost() == null
                || origin.getUserInfo() != null
                || origin.getPath() != null && !origin.getPath().isEmpty()
                || origin.getQuery() != null || origin.getFragment() != null
                || !raw.equals(origin.getScheme() + "://" + origin.getRawAuthority())) {
            throw new IllegalArgumentException("CORS origin must be an explicit HTTP origin");
        }
    }
}

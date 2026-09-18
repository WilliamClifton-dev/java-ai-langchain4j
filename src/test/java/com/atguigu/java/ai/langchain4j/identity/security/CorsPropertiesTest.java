package com.atguigu.java.ai.langchain4j.identity.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorsPropertiesTest {

    @Test
    void acceptsBoundedHttpOrigins() {
        assertThatCode(() -> new CorsProperties(
                List.of("http://localhost:3000", "https://app.example.com"),
                Duration.ofHours(1))).doesNotThrowAnyException();
    }

    @Test
    void rejectsWildcardDuplicateAndNonOriginValues() {
        assertThatThrownBy(() -> new CorsProperties(List.of("*"), Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CorsProperties(
                List.of("http://localhost:3000", "http://localhost:3000"),
                Duration.ofHours(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CorsProperties(
                List.of("https://app.example.com/path"), Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CorsProperties(
                List.of("file:///tmp/app"), Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnboundedMaxAge() {
        assertThatThrownBy(() -> new CorsProperties(
                List.of("http://localhost:3000"), Duration.ofDays(2)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package com.atguigu.java.ai.langchain4j.identity;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.env.MockEnvironment;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthSigningKeyEnvironmentPostProcessorTest {

    private final EnvironmentPostProcessor processor =
            new AuthSigningKeyEnvironmentPostProcessor();

    @Test
    void refusesToMutateEnvironmentAndFailsFastOnDevKeyUnderMinimax() {
        ConfigurableEnvironment environment = new MockEnvironment()
                .withProperty("hbti.auth.signing-key",
                        AuthSigningKeyDefaults.DEFAULT_DEVELOPMENT_KEY);
        environment.setActiveProfiles("minimax");

        assertThatThrownBy(() ->
                processor.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("minimax");
    }

    @Test
    void allowsDevKeyWhenOfflineProfileIsActive() {
        ConfigurableEnvironment environment = new MockEnvironment()
                .withProperty("hbti.auth.signing-key",
                        AuthSigningKeyDefaults.DEFAULT_DEVELOPMENT_KEY);
        environment.setActiveProfiles("offline");

        assertThatCode(() ->
                processor.postProcessEnvironment(environment, null))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsDevKeyWhenTestProfileIsActiveAlongsideMinimax() {
        ConfigurableEnvironment environment = new MockEnvironment()
                .withProperty("hbti.auth.signing-key",
                        AuthSigningKeyDefaults.DEFAULT_DEVELOPMENT_KEY);
        environment.setActiveProfiles("minimax", "test");

        assertThatCode(() ->
                processor.postProcessEnvironment(environment, null))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsCustomKeyRegardlessOfActiveProfiles() {
        ConfigurableEnvironment environment = new MockEnvironment()
                .withProperty("hbti.auth.signing-key",
                        "operator-supplied-secret-with-more-than-32-bytes");
        environment.setActiveProfiles("prod");

        assertThatCode(() ->
                processor.postProcessEnvironment(environment, null))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsUnsetKeyRegardlessOfActiveProfiles() {
        ConfigurableEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("minimax");

        assertThatCode(() ->
                processor.postProcessEnvironment(environment, null))
                .doesNotThrowAnyException();
    }

    @Test
    void processorIsListedOnTheBootSpringFactoriesSpi() throws Exception {
        Properties factories = new Properties();
        try (InputStream input = Files.newInputStream(Paths.get(
                "src", "main", "resources", "META-INF", "spring.factories"))) {
            factories.load(input);
        }
        String key = "org.springframework.boot.env.EnvironmentPostProcessor";
        assertThat(factories.getProperty(key))
                .contains(
                        "com.atguigu.java.ai.langchain4j.identity.AuthSigningKeyEnvironmentPostProcessor");
    }
}

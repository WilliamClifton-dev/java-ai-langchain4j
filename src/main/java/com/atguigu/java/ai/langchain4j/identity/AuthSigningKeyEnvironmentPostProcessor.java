package com.atguigu.java.ai.langchain4j.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Spring Boot {@link EnvironmentPostProcessor} that delegates to
 * {@link AuthSigningKeyValidator} once the application properties have been
 * loaded but before the application context refreshes.
 *
 * <p>Failing here means a deployment that mis-configures
 * {@code hbti.auth.signing-key} under a production-grade profile is rejected
 * before any bean is instantiated.
 */
public class AuthSigningKeyEnvironmentPostProcessor
        implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {
        String signingKey = environment.getProperty("hbti.auth.signing-key");
        String[] activeProfiles = environment.getActiveProfiles();
        AuthSigningKeyValidator.validate(signingKey, activeProfiles);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 10;
    }
}

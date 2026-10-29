package com.atguigu.java.ai.langchain4j.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductionConfigValidatorTest {

    @Test
    void acceptsProductionConfigurationWithHttpsSecureCookiesStrongKeyAndRedis() {
        MockEnvironment environment = validProductionEnvironment();
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");

        assertThatCode(() -> validator(environment, connectionFactory).onApplicationEvent(readyEvent()))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsHttpCorsOriginInProduction() {
        MockEnvironment environment = validProductionEnvironment()
                .withProperty("hbti.security.cors.allowed-origins", "https://app.example.com,http://localhost:5173");

        assertThatThrownBy(() -> validator(environment, mock(RedisConnectionFactory.class))
                .onApplicationEvent(readyEvent()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTP CORS origin");
    }

    @Test
    void rejectsHttpCorsOriginRegardlessOfSchemeCase() {
        MockEnvironment environment = validProductionEnvironment()
                .withProperty("hbti.security.cors.allowed-origins", "HTTP://app.example.com");

        assertThatThrownBy(() -> validator(environment, mock(RedisConnectionFactory.class))
                .onApplicationEvent(readyEvent()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTP CORS origin");
    }

    @Test
    void rejectsInsecureCookiesInProduction() {
        MockEnvironment environment = validProductionEnvironment()
                .withProperty("hbti.auth.secure-cookies", "false");

        assertThatThrownBy(() -> validator(environment, mock(RedisConnectionFactory.class))
                .onApplicationEvent(readyEvent()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("安全 Cookie");
    }

    @Test
    void rejectsWeakSigningKeysInProduction() {
        MockEnvironment environment = validProductionEnvironment()
                .withProperty("hbti.auth.signing-key", "too-short");

        assertThatThrownBy(() -> validator(environment, mock(RedisConnectionFactory.class))
                .onApplicationEvent(readyEvent()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("至少 32 字节");
    }

    @Test
    void rejectsUnavailableRedisInProduction() {
        MockEnvironment environment = validProductionEnvironment();
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        when(connectionFactory.getConnection()).thenThrow(new IllegalStateException("connection refused"));

        assertThatThrownBy(() -> validator(environment, connectionFactory).onApplicationEvent(readyEvent()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Redis 连接不可用");
    }

    private MockEnvironment validProductionEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment.setProperty("hbti.security.cors.allowed-origins", "https://app.example.com");
        environment.setProperty("hbti.auth.secure-cookies", "true");
        environment.setProperty("hbti.auth.signing-key", "production-signing-key-padded-to-32-bytes");
        return environment;
    }

    private ProductionConfigValidator validator(
            MockEnvironment environment, RedisConnectionFactory connectionFactory
    ) {
        return new ProductionConfigValidator(environment, connectionFactory);
    }

    private ApplicationReadyEvent readyEvent() {
        return mock(ApplicationReadyEvent.class);
    }
}

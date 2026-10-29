package com.atguigu.java.ai.langchain4j.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 生产环境配置验证器
 * 在应用启动完成后验证生产环境必需的配置项
 */
@Component
public class ProductionConfigValidator implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(ProductionConfigValidator.class);

    private static final int MIN_SIGNING_KEY_LENGTH = 32;
    private static final List<String> PRODUCTION_PROFILES = Arrays.asList("prod", "production");

    private final Environment environment;
    private final RedisConnectionFactory redisConnectionFactory;

    public ProductionConfigValidator(Environment environment, RedisConnectionFactory redisConnectionFactory) {
        this.environment = environment;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        validate();
    }

    void validate() {
        if (isProduction()) {
            log.info("检测到生产环境,执行配置验证...");
            validateHttpsOnlyCors();
            validateSecureCookies();
            validateStrongSigningKey();
            validateRedisAvailable();
            log.info("生产环境配置验证通过");
        }
    }

    private boolean isProduction() {
        String[] activeProfiles = environment.getActiveProfiles();
        return Arrays.stream(activeProfiles)
                .anyMatch(PRODUCTION_PROFILES::contains);
    }

    private void validateHttpsOnlyCors() {
        String corsOrigins = environment.getProperty("hbti.security.cors.allowed-origins", "");
        List<String> origins = Arrays.asList(corsOrigins.split(","));

        for (String origin : origins) {
            String trimmed = origin.trim();
            if (!trimmed.isEmpty() && trimmed.regionMatches(true, 0, "http://", 0, 7)) {
                throw new IllegalStateException(
                        "生产环境不允许 HTTP CORS origin: " + trimmed +
                        ". 请在配置中只使用 HTTPS origins.");
            }
        }
    }

    private void validateSecureCookies() {
        boolean secureCookies = environment.getProperty("hbti.auth.secure-cookies", Boolean.class, true);
        if (!secureCookies) {
            throw new IllegalStateException(
                    "生产环境必须启用安全 Cookie (hbti.auth.secure-cookies=true). " +
                    "当前配置: " + secureCookies);
        }
    }

    private void validateStrongSigningKey() {
        String signingKey = environment.getProperty("hbti.auth.signing-key", "");
        int keyLength = signingKey.getBytes(StandardCharsets.UTF_8).length;

        if (keyLength < MIN_SIGNING_KEY_LENGTH) {
            throw new IllegalStateException(
                    String.format("生产环境签名密钥长度不足. 要求: 至少 %d 字节, 当前: %d 字节",
                            MIN_SIGNING_KEY_LENGTH, keyLength));
        }

        // 检查是否使用了已知的开发密钥
        if (signingKey.equals("dev-secret-key-for-local-testing-only-min-32-bytes-required")) {
            throw new IllegalStateException(
                    "生产环境不允许使用开发测试密钥. 请设置强随机密钥.");
        }
    }

    private void validateRedisAvailable() {
        try (var connection = redisConnectionFactory.getConnection()) {
            connection.ping();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "生产环境 Redis 连接不可用: " + e.getMessage(), e);
        }
    }
}

package com.atguigu.java.ai.langchain4j.config;

import com.atguigu.java.ai.langchain4j.infrastructure.redis.EphemeralStateStore;
import com.atguigu.java.ai.langchain4j.infrastructure.redis.InMemoryEphemeralStateStore;
import com.atguigu.java.ai.langchain4j.infrastructure.redis.RedisEphemeralStateStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Clock;

@Configuration
public class EphemeralStateConfig {

    @Bean
    @Profile("test")
    EphemeralStateStore inMemoryEphemeralStateStore(Clock clock) {
        return new InMemoryEphemeralStateStore(clock);
    }

    @Bean
    @Profile("!test")
    EphemeralStateStore redisEphemeralStateStore(StringRedisTemplate redis) {
        return new RedisEphemeralStateStore(redis);
    }
}

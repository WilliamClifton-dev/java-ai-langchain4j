package com.atguigu.java.ai.langchain4j.coach.api;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/coach")
public class CoachCapabilityController {
    private static final Set<String> CONFIGURED_PROFILES = Set.of("local", "minimax");

    private final Environment environment;

    public CoachCapabilityController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/capabilities")
    public CoachCapabilityResponse capabilities() {
        String profile = Arrays.stream(environment.getActiveProfiles())
                .filter(CONFIGURED_PROFILES::contains)
                .findFirst()
                .orElse("offline");
        boolean minimaxConfigured = !"minimax".equals(profile)
                || hasText(environment.getProperty("ai.chat-model.api-key"));
        boolean available = CONFIGURED_PROFILES.contains(profile) && minimaxConfigured;
        if (!available) {
            return new CoachCapabilityResponse(false, "OFFLINE", "当前环境未配置 AI 模型");
        }
        return new CoachCapabilityResponse(true, profile.toUpperCase(), "智能教练可用");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank() && !value.startsWith("${");
    }
}

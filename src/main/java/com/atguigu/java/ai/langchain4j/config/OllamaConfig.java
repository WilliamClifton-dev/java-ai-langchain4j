package com.atguigu.java.ai.langchain4j.config;

import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
public class OllamaConfig {
    @Bean
    public OllamaChatModel qwen() {
        return OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("qwen:latest")
                .temperature(0.8)
                .timeout(java.time.Duration.ofSeconds(60))
                .build();
    }
}

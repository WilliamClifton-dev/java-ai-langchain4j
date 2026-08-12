package com.atguigu.java.ai.langchain4j.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

@Configuration
public class ChatModelConfig {

    @Bean(name = "chatModel")
    @Profile("local")
    public ChatLanguageModel localChatModel(
            @Qualifier("ollamaChatModel")
            ChatLanguageModel ollamaChatModel) {
        return ollamaChatModel;
    }

    @Bean(name = "chatModel")
    @Profile({"minimax", "typefun"})
    public ChatLanguageModel apiChatModel(
            @Value("${ai.chat-model.api-key}") String apiKey,
            @Value("${ai.chat-model.base-url}") String baseUrl,
            @Value("${ai.chat-model.model-name}") String modelName) {

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.7)
                .timeout(Duration.ofSeconds(60))
                .build();
    }
}

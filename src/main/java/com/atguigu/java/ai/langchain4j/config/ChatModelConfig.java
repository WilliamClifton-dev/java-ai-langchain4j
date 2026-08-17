package com.atguigu.java.ai.langchain4j.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

@Configuration
public class ChatModelConfig {

    @Bean(name = "chatModel")
    @Profile("offline")
    public ChatLanguageModel offlineChatModel() {
        return new ChatLanguageModel() {
            @Override
            public ChatResponse doChat(ChatRequest request) {
                throw new IllegalStateException("External model is disabled");
            }
        };
    }

    @Bean(name = "chatModel")
    @Profile("local")
    public ChatLanguageModel localChatModel(
            @Qualifier("ollamaChatModel")
            ChatLanguageModel ollamaChatModel) {
        return ollamaChatModel;
    }

    @Bean(name = "chatModel")
    @Profile("minimax")
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

    @Bean(name = "streamingChatModel")
    @Profile("local")
    public StreamingChatLanguageModel localStreamingChatModel(
            @Value("${langchain4j.ollama.chat-model.base-url}") String baseUrl,
            @Value("${langchain4j.ollama.chat-model.model-name}") String modelName,
            @Value("${langchain4j.ollama.chat-model.timeout:PT120S}") Duration timeout) {
        return OllamaStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.7)
                .timeout(timeout)
                .build();
    }

    @Bean(name = "streamingChatModel")
    @Profile("minimax")
    public StreamingChatLanguageModel apiStreamingChatModel(
            @Value("${ai.chat-model.api-key}") String apiKey,
            @Value("${ai.chat-model.base-url}") String baseUrl,
            @Value("${ai.chat-model.model-name}") String modelName) {
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.7)
                .timeout(Duration.ofSeconds(60))
                .build();
    }
}

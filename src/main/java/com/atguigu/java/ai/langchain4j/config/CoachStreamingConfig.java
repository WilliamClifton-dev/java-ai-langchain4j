package com.atguigu.java.ai.langchain4j.config;

import com.atguigu.java.ai.langchain4j.assistant.HbtiCoachStreamingAgent;
import com.atguigu.java.ai.langchain4j.coach.prompt.ScenePromptRepository;
import com.atguigu.java.ai.langchain4j.coach.streaming.CoachInvocationRegistry;
import com.atguigu.java.ai.langchain4j.coach.streaming.CoachStreamingModel;
import com.atguigu.java.ai.langchain4j.coach.streaming.CoachStreamingService;
import com.atguigu.java.ai.langchain4j.coach.streaming.CoachMetrics;
import com.atguigu.java.ai.langchain4j.coach.streaming.CoachRateGuard;
import com.atguigu.java.ai.langchain4j.coach.streaming.CoachToolProvider;
import com.atguigu.java.ai.langchain4j.coach.streaming.LangChain4jCoachStreamingModel;
import com.atguigu.java.ai.langchain4j.coach.streaming.ModelCircuitBreaker;
import com.atguigu.java.ai.langchain4j.coach.tool.CoachToolContext;
import com.atguigu.java.ai.langchain4j.coach.tool.CoachTools;
import com.atguigu.java.ai.langchain4j.knowledge.ReviewedKnowledgeRetriever;
import com.atguigu.java.ai.langchain4j.infrastructure.redis.EphemeralStateStore;
import com.atguigu.java.ai.langchain4j.store.CoachConversationOwnershipService;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class CoachStreamingConfig {

    @Bean
    CoachInvocationRegistry coachInvocationRegistry() {
        return new CoachInvocationRegistry();
    }

    @Bean
    @Profile({"local", "minimax"})
    CoachToolProvider coachToolProvider(CoachTools tools, CoachToolContext context,
                                        CoachInvocationRegistry registry) {
        return new CoachToolProvider(tools, context, registry);
    }

    @Bean
    @Profile({"local", "minimax"})
    HbtiCoachStreamingAgent hbtiCoachStreamingAgent(
            @Qualifier("streamingChatModel") StreamingChatLanguageModel model,
            ChatMemoryProvider memoryProvider, ReviewedKnowledgeRetriever retriever,
            CoachToolProvider toolProvider) {
        return AiServices.builder(HbtiCoachStreamingAgent.class)
                .streamingChatLanguageModel(model)
                .chatMemoryProvider(memoryProvider)
                .contentRetriever(retriever)
                .toolProvider(toolProvider)
                .maxSequentialToolsInvocations(10)
                .build();
    }

    @Bean
    @Profile({"local", "minimax"})
    CoachStreamingModel langChain4jCoachStreamingModel(
            HbtiCoachStreamingAgent agent, CoachInvocationRegistry registry,
            ScenePromptRepository prompts, Clock clock) {
        return new LangChain4jCoachStreamingModel(agent, registry, prompts, clock);
    }

    @Bean
    @Profile({"test", "offline"})
    CoachStreamingModel unavailableTestStreamingModel() {
        return (request, listener) -> {
            listener.onError(new IllegalStateException("External model is disabled"));
            return () -> { };
        };
    }

    @Bean(destroyMethod = "shutdown")
    ScheduledExecutorService coachStreamScheduler() {
        AtomicInteger sequence = new AtomicInteger();
        ThreadFactory factory = task -> {
            Thread thread = new Thread(task, "coach-stream-timeout-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newScheduledThreadPool(2, factory);
    }

    @Bean
    ModelCircuitBreaker modelCircuitBreaker(
            @Value("${hbti.coach.streaming.circuit-failure-threshold:3}") int threshold,
            @Value("${hbti.coach.streaming.circuit-open-duration:PT30S}") Duration openDuration,
            Clock clock) {
        return new ModelCircuitBreaker(threshold, openDuration, clock);
    }

    @Bean
    CoachRateGuard coachRateGuard(
            EphemeralStateStore store,
            @Value("${hbti.rate.coach.maximum-requests:20}") int maximumRequests,
            @Value("${hbti.rate.coach.window:PT1M}") Duration window) {
        return new CoachRateGuard(store, maximumRequests, window);
    }

    @Bean
    CoachStreamingService coachStreamingService(
            CoachStreamingModel model, CoachRateGuard rateGuard, ModelCircuitBreaker breaker,
            ScheduledExecutorService coachStreamScheduler, Clock clock, CoachMetrics metrics,
            CoachConversationOwnershipService ownership,
            @Value("${hbti.coach.streaming.first-token-timeout:PT5S}") Duration firstTokenTimeout,
            @Value("${hbti.coach.streaming.total-timeout:PT30S}") Duration totalTimeout,
            @Value("${hbti.coach.streaming.max-concurrent:5}") int maxConcurrent) {
        return new CoachStreamingService(model, rateGuard, breaker, coachStreamScheduler,
                firstTokenTimeout, totalTimeout, maxConcurrent, clock, metrics, ownership);
    }
}

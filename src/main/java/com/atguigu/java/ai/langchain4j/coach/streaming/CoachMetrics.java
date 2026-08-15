package com.atguigu.java.ai.langchain4j.coach.streaming;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public final class CoachMetrics {
    private static final Set<String> STREAM_OUTCOMES = Set.of(
            "cancelled", "circuit_open", "completed", "concurrency_limited",
            "first_token_timeout", "ownership_conflict", "ownership_unavailable",
            "provider_error", "rate_limited", "total_timeout"
    );
    private static final Set<String> SSE_EVENTS = Set.of(
            "completion", "error", "metadata", "token"
    );
    private static final Set<String> TOOL_NAMES = Set.of(
            "get_active_plan", "get_daily_summary", "get_weekly_review",
            "record_daily_metric", "record_nutrition", "record_training"
    );
    private static final Set<String> TOOL_OUTCOMES = Set.of(
            "OK", "TOOL_INVALID_ARGUMENT", "TOOL_NOT_FOUND", "TOOL_READ_FAILED",
            "TOOL_UNAUTHORIZED", "TOOL_WRITE_FAILED"
    );

    private final MeterRegistry registry;

    public CoachMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    StreamObservation startStream() {
        return new StreamObservation(Timer.start(registry), Timer.start(registry));
    }

    void recordFirstToken(StreamObservation observation) {
        observation.firstToken().stop(registry.timer("hbti.coach.stream.first_token"));
    }

    void recordStreamFinished(StreamObservation observation, String outcome) {
        observation.duration().stop(registry.timer(
                "hbti.coach.stream.duration", "outcome", bounded(outcome, STREAM_OUTCOMES, "other")));
    }

    void recordToken() {
        registry.counter("hbti.coach.stream.tokens").increment();
    }

    void recordSseEvent(String eventType) {
        registry.counter("hbti.coach.sse.events", "event_type",
                bounded(eventType, SSE_EVENTS, "other")).increment();
    }

    public void recordTool(String toolName, String outcome) {
        registry.counter("hbti.coach.tool.calls",
                "tool", bounded(toolName, TOOL_NAMES, "unknown"),
                "outcome", bounded(outcome, TOOL_OUTCOMES, "TOOL_OTHER")).increment();
    }

    private String bounded(String candidate, Set<String> allowed, String fallback) {
        return candidate != null && allowed.contains(candidate) ? candidate : fallback;
    }

    record StreamObservation(Timer.Sample duration, Timer.Sample firstToken) { }
}

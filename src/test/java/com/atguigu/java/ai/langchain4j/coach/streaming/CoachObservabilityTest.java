package com.atguigu.java.ai.langchain4j.coach.streaming;

import com.atguigu.java.ai.langchain4j.coach.dto.CoachChatCommand;
import com.atguigu.java.ai.langchain4j.coach.model.CoachScene;
import com.atguigu.java.ai.langchain4j.infrastructure.redis.InMemoryEphemeralStateStore;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.atguigu.java.ai.langchain4j.store.CoachConversationOwnershipService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CoachObservabilityTest {

    @Test
    void recordsCompletedStreamFirstTokenTokensAndSseEvents() {
        Harness harness = harness();
        RecordingSink sink = new RecordingSink();

        harness.service.open(command(), sink);
        harness.meterClock.add(Duration.ofMillis(25));
        harness.model.listener.onToken("first");
        harness.meterClock.add(Duration.ofMillis(10));
        harness.model.listener.onToken("second");
        harness.model.listener.onComplete();

        assertThat(harness.registry.get("hbti.coach.stream.duration")
                .tag("outcome", "completed").timer().count()).isEqualTo(1);
        assertThat(harness.registry.get("hbti.coach.stream.first_token")
                .timer().count()).isEqualTo(1);
        assertThat(harness.registry.get("hbti.coach.stream.tokens")
                .counter().count()).isEqualTo(2);
        assertThat(sseCount(harness, "metadata")).isEqualTo(1);
        assertThat(sseCount(harness, "token")).isEqualTo(2);
        assertThat(sseCount(harness, "completion")).isEqualTo(1);
    }

    @Test
    void recordsTimeoutAndCancellationAsSingleBoundedOutcomes() {
        Harness timeout = harness();
        timeout.service.open(command(), new RecordingSink());
        timeout.scheduled.get(0).run();
        timeout.model.listener.onComplete();

        assertThat(timeout.registry.get("hbti.coach.stream.duration")
                .tag("outcome", "first_token_timeout").timer().count()).isEqualTo(1);
        assertThat(sseCount(timeout, "error")).isEqualTo(1);

        Harness cancelled = harness();
        CoachStreamSession session = cancelled.service.open(command(), new RecordingSink());
        session.cancel();
        session.cancel();

        assertThat(cancelled.registry.get("hbti.coach.stream.duration")
                .tag("outcome", "cancelled").timer().count()).isEqualTo(1);
    }

    private double sseCount(Harness harness, String eventType) {
        return harness.registry.get("hbti.coach.sse.events")
                .tag("event_type", eventType).counter().count();
    }

    private Harness harness() {
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        List<Runnable> scheduled = new ArrayList<>();
        when(scheduler.schedule(any(Runnable.class), anyLong(), eq(TimeUnit.MILLISECONDS)))
                .thenAnswer(invocation -> {
                    scheduled.add(invocation.getArgument(0));
                    return mock(ScheduledFuture.class);
                });
        Clock appClock = Clock.fixed(Instant.parse("2026-08-15T00:00:00Z"), ZoneOffset.UTC);
        MockClock meterClock = new MockClock();
        SimpleMeterRegistry registry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, meterClock);
        FakeModel model = new FakeModel();
        CoachMetrics metrics = new CoachMetrics(registry);
        CoachStreamingService service = new CoachStreamingService(
                model,
                new CoachRateGuard(new InMemoryEphemeralStateStore(appClock),
                        100, Duration.ofMinutes(1)),
                new ModelCircuitBreaker(3, Duration.ofSeconds(30), appClock),
                scheduler, Duration.ofSeconds(5), Duration.ofSeconds(30), 2, appClock, metrics,
                mock(CoachConversationOwnershipService.class));
        return new Harness(service, model, scheduled, registry, meterClock);
    }

    private CoachChatCommand command() {
        return new CoachChatCommand(
                "user-1", "conversation-1", CoachScene.GENERAL_CHAT, "hello");
    }

    private record Harness(
            CoachStreamingService service,
            FakeModel model,
            List<Runnable> scheduled,
            SimpleMeterRegistry registry,
            MockClock meterClock
    ) { }

    private static final class FakeModel implements CoachStreamingModel {
        private CoachModelListener listener;

        @Override
        public CoachStreamHandle start(CoachModelRequest request, CoachModelListener listener) {
            this.listener = listener;
            return () -> { };
        }
    }

    private static final class RecordingSink implements CoachEventSink {
        @Override public void metadata(String conversationId, CoachScene scene) { }
        @Override public void token(long sequence, String text) { }
        @Override public void completion(String conversationId) { }
        @Override public void error(String code, boolean retryable) { }
    }
}

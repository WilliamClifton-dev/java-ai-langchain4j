package com.atguigu.java.ai.langchain4j.coach.streaming;

import com.atguigu.java.ai.langchain4j.coach.dto.CoachChatCommand;
import com.atguigu.java.ai.langchain4j.coach.model.CoachScene;
import com.atguigu.java.ai.langchain4j.coach.service.CoachMemoryKey;
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
import java.util.concurrent.atomic.AtomicInteger;
import com.atguigu.java.ai.langchain4j.infrastructure.redis.InMemoryEphemeralStateStore;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.atguigu.java.ai.langchain4j.store.CoachConversationOwnershipService;
import com.atguigu.java.ai.langchain4j.store.ConversationOwnershipException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CoachStreamingServiceTest {

    @Test
    void emitsOrderedEventsAndCancelsUpstreamAtMostOnce() {
        FakeModel model = new FakeModel();
        TestHarness harness = harness(model, 3);
        RecordingSink sink = new RecordingSink();

        CoachStreamSession session = harness.service.open(command(), sink);
        model.listener.onToken("先记录");
        model.listener.onToken("一周。");
        model.listener.onComplete();
        session.cancel();

        assertThat(model.request.userId()).isEqualTo("user-1");
        assertThat(model.request.requestNonce()).isNotBlank();
        verify(harness.ownership).claim("user-1", model.request.memoryId());
        assertThat(sink.events).containsExactly(
                "metadata:conversation-1:GENERAL_CHAT",
                "token:1:先记录",
                "token:2:一周。",
                "completion:conversation-1"
        );
        assertThat(model.cancellations).hasValue(0);
    }

    @Test
    void firstTokenTimeoutCancelsModelAndSuppressesLateCallbacks() {
        FakeModel model = new FakeModel();
        TestHarness harness = harness(model, 3);
        RecordingSink sink = new RecordingSink();

        harness.service.open(command(), sink);
        harness.scheduled.get(0).run();
        model.listener.onToken("late");
        model.listener.onComplete();

        assertThat(model.cancellations).hasValue(1);
        assertThat(sink.events).containsExactly(
                "metadata:conversation-1:GENERAL_CHAT",
                "error:MODEL_FIRST_TOKEN_TIMEOUT:true"
        );
    }

    @Test
    void totalTimeoutStillAppliesAfterTheFirstToken() {
        FakeModel model = new FakeModel();
        TestHarness harness = harness(model, 3);
        RecordingSink sink = new RecordingSink();

        harness.service.open(command(), sink);
        model.listener.onToken("started");
        harness.scheduled.get(1).run();
        model.listener.onComplete();

        assertThat(model.cancellations).hasValue(1);
        assertThat(sink.events).containsExactly(
                "metadata:conversation-1:GENERAL_CHAT",
                "token:1:started",
                "error:MODEL_TIMEOUT:true"
        );
    }

    @Test
    void repeatedProviderFailuresOpenCircuitAndFailFast() {
        FakeModel model = new FakeModel();
        TestHarness harness = harness(model, 2);

        RecordingSink first = new RecordingSink();
        harness.service.open(command(), first);
        model.listener.onError(new IllegalStateException("provider secret detail"));

        RecordingSink second = new RecordingSink();
        harness.service.open(command(), second);
        model.listener.onError(new IllegalStateException("another detail"));

        int startsBeforeOpenCall = model.starts.get();
        RecordingSink rejected = new RecordingSink();
        harness.service.open(command(), rejected);

        assertThat(model.starts).hasValue(startsBeforeOpenCall);
        assertThat(rejected.events).containsExactly("error:MODEL_CIRCUIT_OPEN:true");
        assertThat(String.join("|", first.events)).doesNotContain("secret detail");
    }

    @Test
    void ownershipConflictFailsBeforeMetadataAndModelStartWithoutLeakingDetails() {
        FakeModel model = new FakeModel();
        TestHarness harness = harness(model, 3);
        RecordingSink sink = new RecordingSink();
        doThrow(new ConversationOwnershipException()).when(harness.ownership)
                .claim("user-1", CoachMemoryKey.forOwner("user-1", "conversation-1"));

        harness.service.open(command(), sink);

        assertThat(model.starts).hasValue(0);
        assertThat(sink.events).containsExactly("error:CONVERSATION_ACCESS_DENIED:false");
        assertThat(String.join("|", sink.events)).doesNotContain("ownership conflict");
    }

    private TestHarness harness(FakeModel model, int failureThreshold) {
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        List<Runnable> scheduled = new ArrayList<>();
        when(scheduler.schedule(any(Runnable.class), anyLong(), eq(TimeUnit.MILLISECONDS)))
                .thenAnswer(invocation -> {
                    scheduled.add(invocation.getArgument(0));
                    return mock(ScheduledFuture.class);
                });
        Clock clock = Clock.fixed(Instant.parse("2026-08-15T00:00:00Z"), ZoneOffset.UTC);
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(
                failureThreshold, Duration.ofSeconds(30), clock);
        CoachRateGuard rateGuard = new CoachRateGuard(
                new InMemoryEphemeralStateStore(clock), 100, Duration.ofMinutes(1));
        CoachConversationOwnershipService ownership = mock(CoachConversationOwnershipService.class);
        return new TestHarness(new CoachStreamingService(model, rateGuard, breaker, scheduler,
                Duration.ofSeconds(5), Duration.ofSeconds(30), 2, clock,
                new CoachMetrics(new SimpleMeterRegistry()), ownership), scheduled, ownership);
    }

    private CoachChatCommand command() {
        return new CoachChatCommand("user-1", "conversation-1",
                CoachScene.GENERAL_CHAT, "怎么开始？");
    }

    private record TestHarness(
            CoachStreamingService service,
            List<Runnable> scheduled,
            CoachConversationOwnershipService ownership
    ) { }

    private static final class FakeModel implements CoachStreamingModel {
        private final AtomicInteger starts = new AtomicInteger();
        private final AtomicInteger cancellations = new AtomicInteger();
        private CoachModelRequest request;
        private CoachModelListener listener;

        @Override
        public CoachStreamHandle start(CoachModelRequest request, CoachModelListener listener) {
            starts.incrementAndGet();
            this.request = request;
            this.listener = listener;
            return cancellations::incrementAndGet;
        }
    }

    private static final class RecordingSink implements CoachEventSink {
        private final List<String> events = new ArrayList<>();

        @Override
        public void metadata(String conversationId, CoachScene scene) {
            events.add("metadata:" + conversationId + ':' + scene);
        }

        @Override
        public void token(long sequence, String text) {
            events.add("token:" + sequence + ':' + text);
        }

        @Override
        public void completion(String conversationId) {
            events.add("completion:" + conversationId);
        }

        @Override
        public void error(String code, boolean retryable) {
            events.add("error:" + code + ':' + retryable);
        }
    }
}

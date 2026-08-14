package com.atguigu.java.ai.langchain4j.coach.streaming;

import com.atguigu.java.ai.langchain4j.coach.dto.CoachChatCommand;
import com.atguigu.java.ai.langchain4j.coach.service.CoachMemoryKey;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class CoachStreamingService {
    private static final CoachStreamHandle NOOP_HANDLE = () -> { };

    private final CoachStreamingModel model;
    private final ModelCircuitBreaker circuitBreaker;
    private final ScheduledExecutorService scheduler;
    private final Duration firstTokenTimeout;
    private final Duration totalTimeout;
    private final Semaphore concurrency;

    public CoachStreamingService(CoachStreamingModel model, ModelCircuitBreaker circuitBreaker,
                                 ScheduledExecutorService scheduler, Duration firstTokenTimeout,
                                 Duration totalTimeout, int maxConcurrentStreams, Clock clock) {
        if (model == null || circuitBreaker == null || scheduler == null
                || invalid(firstTokenTimeout) || invalid(totalTimeout)
                || totalTimeout.compareTo(firstTokenTimeout) < 0 || maxConcurrentStreams < 1
                || clock == null) {
            throw new IllegalArgumentException("Streaming configuration is invalid");
        }
        this.model = model;
        this.circuitBreaker = circuitBreaker;
        this.scheduler = scheduler;
        this.firstTokenTimeout = firstTokenTimeout;
        this.totalTimeout = totalTimeout;
        this.concurrency = new Semaphore(maxConcurrentStreams);
    }

    public CoachStreamSession open(CoachChatCommand command, CoachEventSink sink) {
        if (command == null || sink == null) throw new IllegalArgumentException("Stream is invalid");
        if (!concurrency.tryAcquire()) {
            sink.error("MODEL_CONCURRENCY_LIMIT", true);
            return () -> { };
        }
        ModelCircuitBreaker.Permit breakerPermit = circuitBreaker.tryAcquire();
        if (!breakerPermit.allowed()) {
            concurrency.release();
            sink.error("MODEL_CIRCUIT_OPEN", true);
            return () -> { };
        }

        StreamState state = new StreamState(command, sink, breakerPermit);
        sink.metadata(command.conversationId(), command.scene());
        state.firstTokenTask.set(schedule(
                () -> state.timeout("MODEL_FIRST_TOKEN_TIMEOUT"), firstTokenTimeout));
        state.totalTask.set(schedule(() -> state.timeout("MODEL_TIMEOUT"), totalTimeout));
        try {
            CoachModelRequest request = new CoachModelRequest(
                    command.userId(), command.conversationId(),
                    CoachMemoryKey.forOwner(command.userId(), command.conversationId()),
                    UUID.randomUUID().toString(), command.scene(), command.message());
            CoachStreamHandle handle = model.start(request, state);
            state.setUpstream(handle == null ? NOOP_HANDLE : handle);
        } catch (RuntimeException failure) {
            state.onError(failure);
        }
        return state;
    }

    private ScheduledFuture<?> schedule(Runnable action, Duration delay) {
        return scheduler.schedule(action, delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    private boolean invalid(Duration value) {
        return value == null || value.isZero() || value.isNegative();
    }

    private final class StreamState implements CoachModelListener, CoachStreamSession {
        private final CoachChatCommand command;
        private final CoachEventSink sink;
        private final ModelCircuitBreaker.Permit breakerPermit;
        private final AtomicBoolean terminal = new AtomicBoolean();
        private final AtomicBoolean upstreamCancelled = new AtomicBoolean();
        private final AtomicLong sequence = new AtomicLong();
        private final AtomicReference<CoachStreamHandle> upstream =
                new AtomicReference<>(NOOP_HANDLE);
        private final AtomicReference<ScheduledFuture<?>> firstTokenTask = new AtomicReference<>();
        private final AtomicReference<ScheduledFuture<?>> totalTask = new AtomicReference<>();

        private StreamState(CoachChatCommand command, CoachEventSink sink,
                            ModelCircuitBreaker.Permit breakerPermit) {
            this.command = command;
            this.sink = sink;
            this.breakerPermit = breakerPermit;
        }

        @Override
        public void onToken(String text) {
            if (terminal.get() || text == null || text.isEmpty()) return;
            cancelTask(firstTokenTask.getAndSet(null));
            if (!terminal.get()) sink.token(sequence.incrementAndGet(), text);
        }

        @Override
        public void onComplete() {
            if (!terminal.compareAndSet(false, true)) return;
            circuitBreaker.onSuccess(breakerPermit);
            cleanup();
            sink.completion(command.conversationId());
        }

        @Override
        public void onError(Throwable failure) {
            fail("MODEL_UNAVAILABLE", true, false);
        }

        private void timeout(String code) {
            fail(code, true, true);
        }

        private void fail(String code, boolean retryable, boolean cancelUpstream) {
            if (!terminal.compareAndSet(false, true)) return;
            circuitBreaker.onFailure(breakerPermit);
            if (cancelUpstream) cancelUpstream();
            cleanup();
            sink.error(code, retryable);
        }

        @Override
        public void cancel() {
            if (!terminal.compareAndSet(false, true)) return;
            cancelUpstream();
            circuitBreaker.onCancellation(breakerPermit);
            cleanup();
        }

        private synchronized void cancelUpstream() {
            if (upstreamCancelled.compareAndSet(false, true)) upstream.get().cancel();
        }

        private synchronized void setUpstream(CoachStreamHandle handle) {
            upstream.set(handle);
            if (upstreamCancelled.get()) handle.cancel();
        }

        private void cleanup() {
            cancelTask(firstTokenTask.getAndSet(null));
            cancelTask(totalTask.getAndSet(null));
            concurrency.release();
        }

        private void cancelTask(ScheduledFuture<?> task) {
            if (task != null) task.cancel(false);
        }
    }
}

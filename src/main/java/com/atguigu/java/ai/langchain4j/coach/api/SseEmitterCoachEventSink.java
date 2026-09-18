package com.atguigu.java.ai.langchain4j.coach.api;

import com.atguigu.java.ai.langchain4j.coach.model.CoachScene;
import com.atguigu.java.ai.langchain4j.coach.streaming.CoachEventSink;
import com.atguigu.java.ai.langchain4j.coach.streaming.CoachStreamSession;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

final class SseEmitterCoachEventSink implements CoachEventSink {
    private static final Map<String, String> ERROR_MESSAGES = Map.of(
            "MODEL_CIRCUIT_OPEN", "Coach model is temporarily unavailable",
            "MODEL_RATE_LIMITED", "Coach request limit reached",
            "MODEL_CONCURRENCY_LIMIT", "Coach model is busy",
            "MODEL_FIRST_TOKEN_TIMEOUT", "Coach model did not respond in time",
            "MODEL_TIMEOUT", "Coach model response timed out",
            "MODEL_UNAVAILABLE", "Coach model is temporarily unavailable"
    );

    private final SseEmitter emitter = new SseEmitter(0L);
    private final AtomicReference<CoachStreamSession> session = new AtomicReference<>();
    private final AtomicBoolean cancellationPending = new AtomicBoolean();

    SseEmitterCoachEventSink() {
        emitter.onCompletion(this::cancel);
        emitter.onTimeout(() -> {
            cancel();
            emitter.complete();
        });
        emitter.onError(failure -> cancel());
    }

    SseEmitter emitter() {
        return emitter;
    }

    void attach(CoachStreamSession value) {
        CoachStreamSession attached = value == null ? () -> { } : value;
        session.set(attached);
        if (cancellationPending.get()) attached.cancel();
    }

    @Override
    public void metadata(String conversationId, CoachScene scene) {
        send("metadata", new CoachStreamMetadataEvent(conversationId, scene), false);
    }

    @Override
    public void token(long sequence, String text) {
        send("token", new CoachStreamTokenEvent(sequence, text), false);
    }

    @Override
    public void completion(String conversationId) {
        send("completion", new CoachStreamCompletionEvent(conversationId), true);
    }

    @Override
    public void error(String code, boolean retryable) {
        send("error", new CoachStreamErrorEvent(code,
                ERROR_MESSAGES.getOrDefault(code, "Coach stream failed"), retryable), true);
    }

    private void send(String event, Object data, boolean terminal) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
            if (terminal) emitter.complete();
        } catch (IOException | IllegalStateException failure) {
            cancel();
            emitter.complete();
        }
    }

    private void cancel() {
        cancellationPending.set(true);
        CoachStreamSession current = session.get();
        if (current != null) current.cancel();
    }
}

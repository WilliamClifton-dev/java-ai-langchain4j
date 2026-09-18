package com.atguigu.java.ai.langchain4j.coach.streaming;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class CoachInvocationRegistry {
    private final ConcurrentHashMap<String, CoachModelRequest> invocations =
            new ConcurrentHashMap<>();

    public void register(CoachModelRequest request) {
        if (request == null || request.memoryId() == null || request.memoryId().isBlank()) {
            throw new IllegalArgumentException("Coach invocation is invalid");
        }
        if (invocations.putIfAbsent(request.memoryId(), request) != null) {
            throw new IllegalStateException("Coach conversation is already streaming");
        }
    }

    public Optional<CoachModelRequest> find(Object memoryId) {
        if (!(memoryId instanceof String value)) return Optional.empty();
        return Optional.ofNullable(invocations.get(value));
    }

    public void remove(CoachModelRequest request) {
        if (request != null) invocations.remove(request.memoryId(), request);
    }
}

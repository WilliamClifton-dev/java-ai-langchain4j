package com.atguigu.java.ai.langchain4j.coach.tool;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class CoachToolContext {
    private final ThreadLocal<Invocation> current = new ThreadLocal<>();

    public <T> T callAs(String userId, String conversationId, Supplier<T> action) {
        return callAs(userId, conversationId, UUID.randomUUID().toString(), action);
    }

    public <T> T callAs(String userId, String conversationId, String requestNonce,
                        Supplier<T> action) {
        if (userId == null || userId.isBlank() || conversationId == null
                || conversationId.isBlank() || requestNonce == null || requestNonce.isBlank()
                || action == null || current.get() != null) {
            throw new IllegalStateException("Coach tool context is invalid");
        }
        current.set(new Invocation(userId, conversationId, requestNonce));
        try {
            return action.get();
        } finally {
            current.remove();
        }
    }

    Optional<Invocation> current() {
        return Optional.ofNullable(current.get());
    }

    record Invocation(String userId, String conversationId, String requestNonce) { }
}

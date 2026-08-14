package com.atguigu.java.ai.langchain4j.assessment;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException() {
        super("Idempotency key was already used with a different payload");
    }
}

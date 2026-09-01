package com.atguigu.java.ai.langchain4j.planning;

public class PlanIdempotencyConflictException extends RuntimeException {

    public PlanIdempotencyConflictException() {
        super("Idempotency key was already used for another plan operation");
    }
}

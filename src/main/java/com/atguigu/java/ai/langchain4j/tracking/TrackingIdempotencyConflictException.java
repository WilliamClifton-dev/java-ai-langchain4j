package com.atguigu.java.ai.langchain4j.tracking;

public class TrackingIdempotencyConflictException extends RuntimeException {
    public TrackingIdempotencyConflictException() { super("Idempotency key payload differs"); }
}

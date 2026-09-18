package com.atguigu.java.ai.langchain4j.infrastructure.redis;

public class EphemeralStateUnavailableException extends RuntimeException {
    public EphemeralStateUnavailableException(Throwable cause) {
        super("Ephemeral state store is unavailable", cause);
    }

    public EphemeralStateUnavailableException() {
        super("Ephemeral state store is unavailable");
    }
}

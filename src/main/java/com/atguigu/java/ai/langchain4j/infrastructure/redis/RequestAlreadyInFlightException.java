package com.atguigu.java.ai.langchain4j.infrastructure.redis;

public class RequestAlreadyInFlightException extends RuntimeException {
    public RequestAlreadyInFlightException() {
        super("An equivalent request is already in flight");
    }
}

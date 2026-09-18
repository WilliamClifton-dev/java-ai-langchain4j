package com.atguigu.java.ai.langchain4j.tracking;

public class TrackingDateConflictException extends RuntimeException {
    public TrackingDateConflictException() { super("A daily record already exists"); }
}

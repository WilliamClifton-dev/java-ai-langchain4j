package com.atguigu.java.ai.langchain4j.coach.streaming;

public class CoachRateLimitExceededException extends RuntimeException {
    public CoachRateLimitExceededException() {
        super("Coach rate limit exceeded");
    }
}

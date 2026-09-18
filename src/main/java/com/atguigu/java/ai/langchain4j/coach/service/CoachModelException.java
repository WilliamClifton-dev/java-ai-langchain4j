package com.atguigu.java.ai.langchain4j.coach.service;

public class CoachModelException extends RuntimeException {
    public CoachModelException(String code) {
        super(code);
    }

    public CoachModelException(String code, Throwable cause) {
        super(code, cause);
    }

    public String code() {
        return getMessage();
    }
}

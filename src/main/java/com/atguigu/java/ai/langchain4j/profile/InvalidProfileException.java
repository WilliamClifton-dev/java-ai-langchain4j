package com.atguigu.java.ai.langchain4j.profile;

public class InvalidProfileException extends IllegalArgumentException {

    public InvalidProfileException(String message) {
        super(message);
    }

    public InvalidProfileException(String message, Throwable cause) {
        super(message, cause);
    }
}

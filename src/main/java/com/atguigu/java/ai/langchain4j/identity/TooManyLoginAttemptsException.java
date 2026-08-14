package com.atguigu.java.ai.langchain4j.identity;

public class TooManyLoginAttemptsException extends RuntimeException {

    public TooManyLoginAttemptsException() {
        super("Too many login attempts");
    }
}

package com.atguigu.java.ai.langchain4j.identity;

public class RefreshTokenReuseException extends RuntimeException {

    public RefreshTokenReuseException() {
        super("Refresh token reuse detected");
    }
}

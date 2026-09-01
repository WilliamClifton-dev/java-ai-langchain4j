package com.atguigu.java.ai.langchain4j.identity;

public class AccountDataNotFoundException extends RuntimeException {
    public AccountDataNotFoundException() {
        super("Account data is unavailable");
    }
}

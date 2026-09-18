package com.atguigu.java.ai.langchain4j.identity;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException() {
        super("An account already exists for this email");
    }
}

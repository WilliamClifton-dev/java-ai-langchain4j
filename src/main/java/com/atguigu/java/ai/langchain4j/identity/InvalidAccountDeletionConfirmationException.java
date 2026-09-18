package com.atguigu.java.ai.langchain4j.identity;

public class InvalidAccountDeletionConfirmationException extends RuntimeException {
    public InvalidAccountDeletionConfirmationException() {
        super("Account deletion confirmation is invalid");
    }
}

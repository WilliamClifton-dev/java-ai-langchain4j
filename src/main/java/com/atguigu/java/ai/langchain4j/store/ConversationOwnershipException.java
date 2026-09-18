package com.atguigu.java.ai.langchain4j.store;

public class ConversationOwnershipException extends RuntimeException {
    public ConversationOwnershipException() {
        super("Conversation ownership conflict");
    }
}

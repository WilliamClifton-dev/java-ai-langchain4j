package com.atguigu.java.ai.langchain4j.store;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CoachConversationOwnershipService {
    private static final int MAX_USER_ID_LENGTH = 36;
    private static final int MAX_MEMORY_ID_LENGTH = 128;

    private final CoachConversationOwnershipMapper mapper;

    CoachConversationOwnershipService(CoachConversationOwnershipMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public void claim(String userId, String memoryId) {
        String owner = bounded(userId, MAX_USER_ID_LENGTH, "User ID");
        String conversation = bounded(memoryId, MAX_MEMORY_ID_LENGTH, "Memory ID");
        mapper.claim(conversation, owner);
        if (mapper.countOwned(conversation, owner) != 1) {
            throw new ConversationOwnershipException();
        }
    }

    private String bounded(String value, int maxLength, String field) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return value;
    }
}

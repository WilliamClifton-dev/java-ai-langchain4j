package com.atguigu.java.ai.langchain4j.store;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
public class MySqlChatMemoryStore implements ChatMemoryStore {

    private static final int MAX_MEMORY_ID_LENGTH = 128;

    private final CoachMessageMapper mapper;

    MySqlChatMemoryStore(CoachMessageMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> getMessages(Object memoryId) {
        String conversationId = normalizeMemoryId(memoryId);
        return mapper.findMessageJsonByConversationId(conversationId).stream()
                .map(ChatMessageDeserializer::messageFromJson)
                .toList();
    }

    @Override
    @Transactional
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String conversationId = normalizeMemoryId(memoryId);
        if (messages == null) {
            throw new IllegalArgumentException("Messages are required");
        }

        mapper.ensureConversation(conversationId);
        mapper.lockConversation(conversationId);
        mapper.deleteMessages(conversationId);

        if (!messages.isEmpty()) {
            List<StoredChatMessage> storedMessages = new ArrayList<>(messages.size());
            for (int index = 0; index < messages.size(); index++) {
                ChatMessage message = messages.get(index);
                if (message == null) {
                    throw new IllegalArgumentException("Messages must not contain null values");
                }
                storedMessages.add(new StoredChatMessage(
                        index,
                        ChatMessageSerializer.messageToJson(message)
                ));
            }
            mapper.insertMessages(conversationId, storedMessages);
        }

        mapper.incrementVersion(conversationId);
    }

    @Override
    @Transactional
    public void deleteMessages(Object memoryId) {
        mapper.deleteConversation(normalizeMemoryId(memoryId));
    }

    private String normalizeMemoryId(Object memoryId) {
        if (memoryId == null) {
            throw new IllegalArgumentException("Memory ID is required");
        }
        String normalized = memoryId.toString().trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Memory ID is required");
        }
        if (normalized.length() > MAX_MEMORY_ID_LENGTH) {
            throw new IllegalArgumentException("Memory ID must not exceed 128 characters");
        }
        return normalized;
    }
}

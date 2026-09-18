package com.atguigu.java.ai.langchain4j.store;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@MybatisTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
@Import(MySqlChatMemoryStore.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MySqlChatMemoryStoreTest {

    private final MySqlChatMemoryStore store;

    @Autowired
    MySqlChatMemoryStoreTest(MySqlChatMemoryStore store) {
        this.store = store;
    }

    @BeforeEach
    void clearConversations() {
        store.deleteMessages("conversation-1");
        store.deleteMessages("conversation-2");
        store.deleteMessages("missing-conversation");
    }

    @Test
    void returnsEmptyMessagesForAnUnknownConversation() {
        assertThat(store.getMessages("missing-conversation")).isEmpty();
    }

    @Test
    void storesMessagesInOrderAndPreservesTheirTypes() {
        List<ChatMessage> expected = List.of(
                SystemMessage.from("system"),
                UserMessage.from("hello"),
                AiMessage.from("welcome")
        );

        store.updateMessages("conversation-1", expected);

        assertThat(store.getMessages("conversation-1"))
                .containsExactlyElementsOf(expected);
    }

    @Test
    void atomicallyReplacesTheCurrentMemoryWindowWithoutMixingConversations() {
        store.updateMessages("conversation-1", List.of(UserMessage.from("old")));
        store.updateMessages("conversation-2", List.of(UserMessage.from("other")));

        store.updateMessages("conversation-1", List.of(
                UserMessage.from("new"),
                AiMessage.from("answer")
        ));

        assertThat(store.getMessages("conversation-1")).containsExactly(
                UserMessage.from("new"),
                AiMessage.from("answer")
        );
        assertThat(store.getMessages("conversation-2"))
                .containsExactly(UserMessage.from("other"));
    }

    @Test
    void deletesTheConversationAndItsMessages() {
        store.updateMessages("conversation-1", List.of(UserMessage.from("hello")));

        store.deleteMessages("conversation-1");

        assertThat(store.getMessages("conversation-1")).isEmpty();
    }

    @Test
    void rollsBackTheReplacementWhenAStoredMessageIsInvalid() {
        store.updateMessages("conversation-1", List.of(UserMessage.from("original")));

        List<ChatMessage> invalidMessages = new java.util.ArrayList<>();
        invalidMessages.add(UserMessage.from("replacement"));
        invalidMessages.add(null);

        assertThatThrownBy(() -> store.updateMessages("conversation-1", invalidMessages))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(store.getMessages("conversation-1"))
                .containsExactly(UserMessage.from("original"));
    }

    @Test
    void rejectsInvalidMemoryIdsBeforeAccessingTheDatabase() {
        assertThatThrownBy(() -> store.getMessages(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.getMessages("   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.getMessages("x".repeat(129)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

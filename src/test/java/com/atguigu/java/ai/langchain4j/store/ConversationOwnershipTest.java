package com.atguigu.java.ai.langchain4j.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@MybatisTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
@Import(CoachConversationOwnershipService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConversationOwnershipTest {

    private static final String USER_ONE = "00000000-0000-4000-8000-000000000101";
    private static final String USER_TWO = "00000000-0000-4000-8000-000000000102";
    private static final String MEMORY_ID = "owned:test-conversation-memory";

    @Autowired
    private CoachConversationOwnershipService ownership;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepareAccounts() {
        jdbcTemplate.update("DELETE FROM user_account WHERE id IN (?, ?)", USER_ONE, USER_TWO);
        insertAccount(USER_ONE, "owner-one@example.com");
        insertAccount(USER_TWO, "owner-two@example.com");
    }

    @Test
    void claimIsIdempotentAndCannotBeTakenOverByAnotherUser() {
        ownership.claim(USER_ONE, MEMORY_ID);
        ownership.claim(USER_ONE, MEMORY_ID);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT user_id FROM coach_conversation WHERE id = ?",
                String.class, MEMORY_ID)).isEqualTo(USER_ONE);
        assertThatThrownBy(() -> ownership.claim(USER_TWO, MEMORY_ID))
                .isInstanceOf(ConversationOwnershipException.class);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT user_id FROM coach_conversation WHERE id = ?",
                String.class, MEMORY_ID)).isEqualTo(USER_ONE);
    }

    @Test
    void deletingTheOwnerCascadesConversationAndMessages() {
        ownership.claim(USER_ONE, MEMORY_ID);
        jdbcTemplate.update("""
                INSERT INTO coach_message (conversation_id, sequence_no, message_json)
                VALUES (?, 1, '{}')
                """, MEMORY_ID);

        jdbcTemplate.update("DELETE FROM user_account WHERE id = ?", USER_ONE);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM coach_conversation WHERE id = ?",
                Integer.class, MEMORY_ID)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM coach_message WHERE conversation_id = ?",
                Integer.class, MEMORY_ID)).isZero();
    }

    private void insertAccount(String id, String email) {
        jdbcTemplate.update("""
                INSERT INTO user_account (
                    id, normalized_email, password_hash, status, created_at, updated_at
                ) VALUES (?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, id, email, "$2a$12$test-only-not-a-real-credential-hash-value");
    }
}

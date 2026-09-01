package com.atguigu.java.ai.langchain4j.common.retention;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RetentionCleanupPersistenceTest {

    @Autowired RetentionMapper mapper;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void deletesOnlyRowsOlderThanTheSuppliedCutoffs() {
        Instant now = Instant.parse("2026-08-17T12:00:00Z");
        String userId = "00000000-0000-4000-8000-000000000701";
        jdbcTemplate.update("""
                INSERT INTO user_account
                    (id, normalized_email, password_hash, status, created_at, updated_at)
                VALUES (?, ?, ?, 'ACTIVE', ?, ?)
                """, userId, "retention@example.com", "$2a$12$test-retention-hash",
                Timestamp.from(now), Timestamp.from(now));
        insertToken("old-token", userId, now.minusSeconds(8 * 86_400L), now);
        insertToken("recent-token", userId, now.minusSeconds(6 * 86_400L), now);
        insertAudit("old-audit", userId, now.minusSeconds(181 * 86_400L));
        insertAudit("recent-audit", userId, now.minusSeconds(179 * 86_400L));

        assertThat(mapper.deleteExpiredRefreshTokens(now.minusSeconds(7 * 86_400L))).isEqualTo(1);
        assertThat(mapper.deleteExpiredAuditEvents(now.minusSeconds(180 * 86_400L))).isEqualTo(1);

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM refresh_token", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_event", Integer.class)).isEqualTo(1);
    }

    private void insertToken(String id, String userId, Instant expiresAt, Instant createdAt) {
        jdbcTemplate.update("""
                INSERT INTO refresh_token
                    (id, user_id, token_hash, family_id, expires_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, userId, String.format("%064d", id.hashCode() & 0x7fffffff),
                "family-" + id, Timestamp.from(expiresAt), Timestamp.from(createdAt));
    }

    private void insertAudit(String details, String userId, Instant eventTime) {
        jdbcTemplate.update("""
                INSERT INTO audit_event (event_type, user_id, event_time, success, details)
                VALUES ('LOGIN_SUCCESS', ?, ?, TRUE, ?)
                """, userId, Timestamp.from(eventTime), details);
    }
}

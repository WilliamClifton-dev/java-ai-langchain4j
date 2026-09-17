package com.atguigu.java.ai.langchain4j.common.retention;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.time.Instant;

@Mapper
public interface RetentionMapper {

    int BATCH_SIZE = 500;

    @Delete("DELETE FROM refresh_token WHERE expires_at < #{cutoff} LIMIT " + BATCH_SIZE)
    int deleteExpiredRefreshTokens(Instant cutoff);

    @Delete("DELETE FROM audit_event WHERE event_time < #{cutoff} LIMIT " + BATCH_SIZE)
    int deleteExpiredAuditEvents(Instant cutoff);

    @Delete("DELETE FROM coach_conversation WHERE last_message_at < #{cutoff} LIMIT " + BATCH_SIZE)
    int deleteInactiveConversations(Instant cutoff);
}

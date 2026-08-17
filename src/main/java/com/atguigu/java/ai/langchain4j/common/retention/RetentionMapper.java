package com.atguigu.java.ai.langchain4j.common.retention;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.time.Instant;

@Mapper
public interface RetentionMapper {

    @Delete("DELETE FROM refresh_token WHERE expires_at < #{cutoff}")
    int deleteExpiredRefreshTokens(Instant cutoff);

    @Delete("DELETE FROM audit_event WHERE event_time < #{cutoff}")
    int deleteExpiredAuditEvents(Instant cutoff);
}

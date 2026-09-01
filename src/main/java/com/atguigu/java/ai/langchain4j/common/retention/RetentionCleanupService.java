package com.atguigu.java.ai.langchain4j.common.retention;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class RetentionCleanupService {

    private final RetentionMapper mapper;
    private final Clock clock;
    private final Duration refreshTokenGrace;
    private final Duration auditRetention;

    public RetentionCleanupService(
            RetentionMapper mapper,
            Clock clock,
            @Value("${hbti.retention.refresh-token-grace:P7D}") Duration refreshTokenGrace,
            @Value("${hbti.retention.audit-events:P180D}") Duration auditRetention) {
        if (refreshTokenGrace.isNegative() || auditRetention.isNegative() || auditRetention.isZero()) {
            throw new IllegalArgumentException("Retention durations must be bounded and positive");
        }
        this.mapper = mapper;
        this.clock = clock;
        this.refreshTokenGrace = refreshTokenGrace;
        this.auditRetention = auditRetention;
    }

    @Transactional
    public RetentionCleanupResult purgeExpiredData() {
        Instant now = clock.instant();
        int tokens = mapper.deleteExpiredRefreshTokens(now.minus(refreshTokenGrace));
        int audits = mapper.deleteExpiredAuditEvents(now.minus(auditRetention));
        return new RetentionCleanupResult(tokens, audits, now);
    }

    public record RetentionCleanupResult(
            int refreshTokensDeleted,
            int auditEventsDeleted,
            Instant completedAt) {
    }
}

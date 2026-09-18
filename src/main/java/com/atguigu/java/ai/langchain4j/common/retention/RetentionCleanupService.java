package com.atguigu.java.ai.langchain4j.common.retention;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.function.IntSupplier;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class RetentionCleanupService {

    private final RetentionMapper mapper;
    private final Clock clock;
    private final Duration refreshTokenGrace;
    private final Duration auditRetention;
    private final Duration conversationRetention;
    private final TransactionTemplate batchTransaction;

    public RetentionCleanupService(
            RetentionMapper mapper,
            Clock clock,
            @Value("${hbti.retention.refresh-token-grace:P7D}") Duration refreshTokenGrace,
            @Value("${hbti.retention.audit-events:P180D}") Duration auditRetention,
            @Value("${hbti.retention.conversations:P90D}") Duration conversationRetention,
            PlatformTransactionManager transactionManager) {
        if (refreshTokenGrace.isNegative() || auditRetention.isNegative() || auditRetention.isZero()
                || conversationRetention.isNegative() || conversationRetention.isZero()) {
            throw new IllegalArgumentException("Retention durations must be bounded and positive");
        }
        this.mapper = mapper;
        this.clock = clock;
        this.refreshTokenGrace = refreshTokenGrace;
        this.auditRetention = auditRetention;
        this.conversationRetention = conversationRetention;
        this.batchTransaction = new TransactionTemplate(transactionManager);
        this.batchTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public RetentionCleanupResult purgeExpiredData() {
        Instant now = clock.instant();
        int tokens = purgeBatches(() -> mapper.deleteExpiredRefreshTokens(now.minus(refreshTokenGrace)));
        int audits = purgeBatches(() -> mapper.deleteExpiredAuditEvents(now.minus(auditRetention)));
        int conversations = purgeBatches(() -> mapper.deleteInactiveConversations(now.minus(conversationRetention)));
        return new RetentionCleanupResult(tokens, audits, conversations, now);
    }

    private int purgeBatches(IntSupplier delete) {
        int total = 0;
        int deleted;
        do {
            deleted = batchTransaction.execute(status -> delete.getAsInt());
            total = Math.addExact(total, deleted);
        } while (deleted == RetentionMapper.BATCH_SIZE);
        return total;
    }

    public record RetentionCleanupResult(
            int refreshTokensDeleted,
            int auditEventsDeleted,
            int conversationsDeleted,
            Instant completedAt) {
    }
}

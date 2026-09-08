package com.atguigu.java.ai.langchain4j.common.retention;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.TransactionDefinition;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RetentionCleanupServiceTest {

    @Test
    void purgesOnlyPastTheExactTokenGraceAndAuditRetentionCutoffs() {
        RetentionMapper mapper = mock(RetentionMapper.class);
        Instant now = Instant.parse("2026-08-17T12:00:00Z");
        Instant tokenCutoff = now.minus(Duration.ofDays(7));
        Instant auditCutoff = now.minus(Duration.ofDays(180));
        when(mapper.deleteExpiredRefreshTokens(tokenCutoff)).thenReturn(3);
        when(mapper.deleteExpiredAuditEvents(auditCutoff)).thenReturn(4);
        RetentionCleanupService service = new RetentionCleanupService(
                mapper, Clock.fixed(now, ZoneOffset.UTC), Duration.ofDays(7), Duration.ofDays(180), new RecordingTransactions());

        RetentionCleanupService.RetentionCleanupResult result = service.purgeExpiredData();

        assertThat(result.refreshTokensDeleted()).isEqualTo(3);
        assertThat(result.auditEventsDeleted()).isEqualTo(4);
        assertThat(result.completedAt()).isEqualTo(now);
        verify(mapper).deleteExpiredRefreshTokens(tokenCutoff);
        verify(mapper).deleteExpiredAuditEvents(auditCutoff);
    }

    @Test
    void commitsEachBatchAndCountsAllExpiredRows() {
        RetentionMapper mapper = mock(RetentionMapper.class);
        Instant now = Instant.parse("2026-09-08T00:00:00Z");
        when(mapper.deleteExpiredRefreshTokens(now.minus(Duration.ofDays(7)))).thenReturn(500, 2);
        when(mapper.deleteExpiredAuditEvents(now.minus(Duration.ofDays(180)))).thenReturn(500, 500, 1);
        RecordingTransactions transactions = new RecordingTransactions();
        var result = new RetentionCleanupService(mapper, Clock.fixed(now, ZoneOffset.UTC),
                Duration.ofDays(7), Duration.ofDays(180), transactions).purgeExpiredData();
        assertThat(result.refreshTokensDeleted()).isEqualTo(502);
        assertThat(result.auditEventsDeleted()).isEqualTo(1001);
        assertThat(transactions.commits).isEqualTo(5);
    }

    private static final class RecordingTransactions extends AbstractPlatformTransactionManager {
        int commits;
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) { }
        @Override protected void doCommit(DefaultTransactionStatus status) { commits++; }
        @Override protected void doRollback(DefaultTransactionStatus status) { }
    }
}

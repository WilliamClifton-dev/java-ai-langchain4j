package com.atguigu.java.ai.langchain4j.common.retention;

import org.junit.jupiter.api.Test;

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
                mapper, Clock.fixed(now, ZoneOffset.UTC), Duration.ofDays(7), Duration.ofDays(180));

        RetentionCleanupService.RetentionCleanupResult result = service.purgeExpiredData();

        assertThat(result.refreshTokensDeleted()).isEqualTo(3);
        assertThat(result.auditEventsDeleted()).isEqualTo(4);
        assertThat(result.completedAt()).isEqualTo(now);
        verify(mapper).deleteExpiredRefreshTokens(tokenCutoff);
        verify(mapper).deleteExpiredAuditEvents(auditCutoff);
    }
}

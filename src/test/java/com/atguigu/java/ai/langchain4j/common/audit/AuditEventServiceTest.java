package com.atguigu.java.ai.langchain4j.common.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditEventServiceTest {

    private AuditEventService auditService;
    private AuditEventMapper mockMapper;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        mockMapper = mock(AuditEventMapper.class);
        meterRegistry = new SimpleMeterRegistry();
        auditService = new AuditEventService(
                mockMapper, new com.fasterxml.jackson.databind.ObjectMapper(), meterRegistry);
    }

    @Test
    void recordsEventWithoutSensitiveDetails() {
        AuditEvent event = AuditEvent.create(
                AuditEventType.LOGIN_SUCCESS,
                "user-123",
                "192.0.2.1",
                true,
                Map.of("action", "login", "password", "secret", "ip", "192.0.2.1")
        );

        auditService.record(event);

        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockMapper).insert(
                eq("LOGIN_SUCCESS"),
                eq("user-123"),
                eq("192.0.2.1"),
                isNull(),
                eq(true),
                detailsCaptor.capture()
        );

        String details = detailsCaptor.getValue();
        assertThat(details).doesNotContain("password");
        assertThat(details).doesNotContain("secret");
        assertThat(details).contains("action");
        assertThat(details).doesNotContain("ip");
    }

    @Test
    void handlesNullDetailsGracefully() {
        AuditEvent event = AuditEvent.create(
                AuditEventType.LOGOUT,
                "user-456",
                null,
                true,
                null
        );

        assertThatCode(() -> auditService.record(event)).doesNotThrowAnyException();
        verify(mockMapper).insert(
                eq("LOGOUT"), eq("user-456"), isNull(), isNull(), eq(true), isNull());
    }

    @Test
    void ignoresInvalidEvents() {
        assertThatCode(() -> auditService.record(null)).doesNotThrowAnyException();
        assertThatCode(() -> auditService.record(
                new AuditEvent(null, null, "user", null, null, true, null)
        )).doesNotThrowAnyException();
        verify(mockMapper, never()).insert(any(), any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void handlesMapperFailuresGracefully() {
        doThrow(new RuntimeException("Database error containing token-canary"))
                .when(mockMapper).insert(any(), any(), any(), any(), anyBoolean(), any());

        AuditEvent event = AuditEvent.create(
                AuditEventType.LOGIN_FAILURE,
                "user-789",
                "192.0.2.2",
                false,
                Map.of("reason", "invalid password")
        );

        assertThatCode(() -> auditService.record(event)).doesNotThrowAnyException();
        assertThat(meterRegistry.get("hbti.audit.events")
                .tag("event_type", "LOGIN_FAILURE")
                .tag("outcome", "persistence_failed")
                .counter().count()).isEqualTo(1);
    }

    @Test
    void recursivelyRemovesSensitiveFieldsAndBoundsDetails() {
        AuditEvent event = AuditEvent.create(
                AuditEventType.LOGIN_FAILURE,
                null,
                "192.0.2.3",
                false,
                Map.of(
                        "reasonCode", "INVALID_CREDENTIALS",
                        "context", Map.of(
                                "action", "login",
                                "password", "nested-password-canary",
                                "token", "nested-token-canary"
                        ),
                        "values", List.of("safe", Map.of("secret", "nested-secret-canary")),
                        "unapproved", "x".repeat(5000)
                )
        );
        MDC.put("requestId", "audit-request-17");
        try {
            auditService.record(event);
        } finally {
            MDC.remove("requestId");
        }

        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockMapper).insert(
                eq("LOGIN_FAILURE"), isNull(), eq("192.0.2.3"),
                eq("audit-request-17"), eq(false), detailsCaptor.capture());
        assertThat(detailsCaptor.getValue())
                .contains("INVALID_CREDENTIALS", "login", "safe")
                .doesNotContain("password", "token", "secret", "unapproved")
                .hasSizeLessThanOrEqualTo(2000);
        assertThat(meterRegistry.get("hbti.audit.events")
                .tag("event_type", "LOGIN_FAILURE")
                .tag("outcome", "persisted")
                .counter().count()).isEqualTo(1);
    }
}

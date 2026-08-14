package com.atguigu.java.ai.langchain4j.common.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditEventServiceTest {

    private AuditEventService auditService;
    private AuditEventMapper mockMapper;

    @BeforeEach
    void setUp() {
        mockMapper = mock(AuditEventMapper.class);
        auditService = new AuditEventService(mockMapper, new com.fasterxml.jackson.databind.ObjectMapper());
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
                eq(true),
                detailsCaptor.capture()
        );

        String details = detailsCaptor.getValue();
        assertThat(details).doesNotContain("password");
        assertThat(details).doesNotContain("secret");
        assertThat(details).contains("action");
        assertThat(details).contains("ip");
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
        verify(mockMapper).insert(eq("LOGOUT"), eq("user-456"), isNull(), eq(true), isNull());
    }

    @Test
    void ignoresInvalidEvents() {
        assertThatCode(() -> auditService.record(null)).doesNotThrowAnyException();
        assertThatCode(() -> auditService.record(
                new AuditEvent(null, null, "user", null, null, true, null)
        )).doesNotThrowAnyException();
        verify(mockMapper, never()).insert(any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void handlesMapperFailuresGracefully() {
        doThrow(new RuntimeException("Database error")).when(mockMapper).insert(any(), any(), any(), anyBoolean(), any());

        AuditEvent event = AuditEvent.create(
                AuditEventType.LOGIN_FAILURE,
                "user-789",
                "192.0.2.2",
                false,
                Map.of("reason", "invalid password")
        );

        assertThatCode(() -> auditService.record(event)).doesNotThrowAnyException();
    }
}

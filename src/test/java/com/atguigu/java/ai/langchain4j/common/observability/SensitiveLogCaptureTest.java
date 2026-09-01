package com.atguigu.java.ai.langchain4j.common.observability;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.atguigu.java.ai.langchain4j.common.audit.AuditEvent;
import com.atguigu.java.ai.langchain4j.common.audit.AuditEventMapper;
import com.atguigu.java.ai.langchain4j.common.audit.AuditEventService;
import com.atguigu.java.ai.langchain4j.common.audit.AuditEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class SensitiveLogCaptureTest {

    @Test
    void requestLogContainsBoundedFieldsButNoHeadersBodyOrRawPath() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(RequestCorrelationFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            MockHttpServletRequest request = new MockHttpServletRequest(
                    "POST", "/api/v1/coach/stream/private-user-path");
            request.addHeader("Authorization", "Bearer token-canary-7219");
            request.addHeader("Cookie", "HBTI_REFRESH=refresh-canary-4821");
            request.setContent("{\"password\":\"password-canary-9043\"}".getBytes());
            MockHttpServletResponse response = new MockHttpServletResponse();
            response.setStatus(204);

            new RequestCorrelationFilter().doFilter(request, response, (incoming, outgoing) -> { });

            String rendered = appender.list.stream()
                    .map(event -> event.getFormattedMessage() + event.getMDCPropertyMap()
                            + event.getKeyValuePairs())
                    .reduce("", (left, right) -> left + right);
            assertThat(rendered)
                    .contains("event=\"http_request_completed\"", "method=\"POST\"",
                            "status_class=\"2xx\"")
                    .doesNotContain("private-user-path", "token-canary-7219",
                            "refresh-canary-4821", "password-canary-9043");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void auditFailureLogDoesNotRenderDatabaseExceptionDetails() {
        Logger logger = (Logger) LoggerFactory.getLogger(AuditEventService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            AuditEventMapper mapper = mock(AuditEventMapper.class);
            doThrow(new IllegalStateException(
                    "Authorization Bearer audit-token-canary-4921 password-canary-5107"))
                    .when(mapper).insert(any(), any(), any(), any(), anyBoolean(), any());
            AuditEventService service = new AuditEventService(
                    mapper, new ObjectMapper(), new SimpleMeterRegistry());

            service.record(AuditEvent.create(
                    AuditEventType.LOGIN_FAILURE, null, "192.0.2.4", false,
                    java.util.Map.of("reasonCode", "INVALID_CREDENTIALS")));

            String rendered = appender.list.stream()
                    .map(event -> event.getFormattedMessage() + event.getKeyValuePairs())
                    .reduce("", (left, right) -> left + right);
            assertThat(rendered)
                    .contains("event=\"audit_persistence_failed\"",
                            "event_type=\"LOGIN_FAILURE\"")
                    .doesNotContain("audit-token-canary-4921", "password-canary-5107",
                            "Authorization", "Bearer");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}

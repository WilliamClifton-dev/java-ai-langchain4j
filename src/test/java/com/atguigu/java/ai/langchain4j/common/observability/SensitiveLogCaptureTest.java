package com.atguigu.java.ai.langchain4j.common.observability;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

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
                    .map(event -> event.getFormattedMessage() + event.getMDCPropertyMap())
                    .reduce("", (left, right) -> left + right);
            assertThat(rendered)
                    .contains("event=http_request_completed", "method=POST", "status_class=2xx")
                    .doesNotContain("private-user-path", "token-canary-7219",
                            "refresh-canary-4821", "password-canary-9043");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}

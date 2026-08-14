package com.atguigu.java.ai.langchain4j.common.observability;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test
    void propagatesBoundedRequestIdAndClearsMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/plans/active");
        request.addHeader(RequestCorrelationFilter.HEADER_NAME, "client-request_42");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdInsideChain = new AtomicReference<>();
        FilterChain chain = (incoming, outgoing) ->
                requestIdInsideChain.set(MDC.get(RequestCorrelationFilter.MDC_KEY));

        filter.doFilter(request, response, chain);

        assertThat(requestIdInsideChain).hasValue("client-request_42");
        assertThat(response.getHeader(RequestCorrelationFilter.HEADER_NAME))
                .isEqualTo("client-request_42");
        assertThat(MDC.get(RequestCorrelationFilter.MDC_KEY)).isNull();
    }

    @Test
    void replacesUntrustedRequestIdWithUuid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.addHeader(RequestCorrelationFilter.HEADER_NAME,
                "line-break\r\nAuthorization: Bearer sensitive-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdInsideChain = new AtomicReference<>();

        filter.doFilter(request, response, (incoming, outgoing) ->
                requestIdInsideChain.set(MDC.get(RequestCorrelationFilter.MDC_KEY)));

        assertThat(requestIdInsideChain.get())
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
                .doesNotContain("sensitive-token");
        assertThat(response.getHeader(RequestCorrelationFilter.HEADER_NAME))
                .isEqualTo(requestIdInsideChain.get());
        assertThat(MDC.get(RequestCorrelationFilter.MDC_KEY)).isNull();
    }

    @Test
    void clearsMdcWhenDownstreamFails() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/profile");
        MockHttpServletResponse response = new MockHttpServletResponse();

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                filter.doFilter(request, response, (incoming, outgoing) -> {
                    throw new IllegalStateException("downstream failure");
                })).isInstanceOf(IllegalStateException.class);

        assertThat(response.getHeader(RequestCorrelationFilter.HEADER_NAME)).isNotBlank();
        assertThat(MDC.get(RequestCorrelationFilter.MDC_KEY)).isNull();
    }
}

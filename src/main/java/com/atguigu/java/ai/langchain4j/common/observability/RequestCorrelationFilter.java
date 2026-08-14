package com.atguigu.java.ai.langchain4j.common.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {
    public static final String HEADER_NAME = "X-Request-ID";
    public static final String MDC_KEY = "requestId";

    private static final Logger log = LoggerFactory.getLogger(RequestCorrelationFilter.class);
    private static final Pattern ACCEPTED_ID = Pattern.compile("[A-Za-z0-9._:-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = trustedOrGenerated(request.getHeader(HEADER_NAME));
        long startedNanos = System.nanoTime();
        response.setHeader(HEADER_NAME, requestId);
        MDC.put(MDC_KEY, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMillis = (System.nanoTime() - startedNanos) / 1_000_000L;
            log.info("event=http_request_completed method={} status_class={} duration_ms={}",
                    request.getMethod(), statusClass(response.getStatus()), durationMillis);
            MDC.remove(MDC_KEY);
        }
    }

    private String trustedOrGenerated(String candidate) {
        if (candidate != null && ACCEPTED_ID.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }

    private String statusClass(int status) {
        int boundedStatus = status >= 100 && status <= 599 ? status : 500;
        return (boundedStatus / 100) + "xx";
    }
}

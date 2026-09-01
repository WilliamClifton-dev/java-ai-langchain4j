package com.atguigu.java.ai.langchain4j.common.audit;

import java.time.Instant;
import java.util.Map;

public record AuditEvent(
        Long id,
        AuditEventType eventType,
        String userId,
        String remoteAddress,
        Instant eventTime,
        boolean success,
        Map<String, Object> details
) {
    public static AuditEvent create(
            AuditEventType eventType,
            String userId,
            String remoteAddress,
            boolean success,
            Map<String, Object> details
    ) {
        return new AuditEvent(null, eventType, userId, remoteAddress, null, success, details);
    }
}

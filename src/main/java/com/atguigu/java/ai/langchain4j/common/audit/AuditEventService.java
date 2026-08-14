package com.atguigu.java.ai.langchain4j.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuditEventService {
    private static final Logger log = LoggerFactory.getLogger(AuditEventService.class);
    private final AuditEventMapper mapper;
    private final ObjectMapper objectMapper;

    public AuditEventService(AuditEventMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    public void record(AuditEvent event) {
        if (event == null || event.eventType() == null || event.userId() == null) {
            return;
        }
        try {
            String detailsJson = event.details() == null || event.details().isEmpty()
                    ? null
                    : objectMapper.writeValueAsString(sanitize(event.details()));
            mapper.insert(
                    event.eventType().name(),
                    event.userId(),
                    event.remoteAddress(),
                    event.success(),
                    detailsJson
            );
        } catch (JsonProcessingException exception) {
            log.warn("Failed to serialize audit details", exception);
        } catch (RuntimeException exception) {
            log.error("Failed to record audit event", exception);
        }
    }

    private Map<String, Object> sanitize(Map<String, Object> details) {
        return details.entrySet().stream()
                .filter(entry -> !isSensitive(entry.getKey()))
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

    private boolean isSensitive(String key) {
        String lower = key.toLowerCase();
        return lower.contains("password") || lower.contains("token")
                || lower.contains("secret") || lower.contains("key");
    }
}

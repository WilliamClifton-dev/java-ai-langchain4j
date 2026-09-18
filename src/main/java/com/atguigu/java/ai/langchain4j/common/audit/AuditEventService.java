package com.atguigu.java.ai.langchain4j.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AuditEventService {
    private static final int MAX_DEPTH = 3;
    private static final int MAX_COLLECTION_SIZE = 10;
    private static final int MAX_STRING_LENGTH = 128;
    private static final int MAX_DETAILS_LENGTH = 2000;
    private static final Set<String> ALLOWED_DETAIL_KEYS = Set.of(
            "action", "context", "outcome", "planId", "reasonCode", "values", "versionId"
    );
    private static final Logger log = LoggerFactory.getLogger(AuditEventService.class);
    private final AuditEventMapper mapper;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public AuditEventService(
            AuditEventMapper mapper, ObjectMapper objectMapper, MeterRegistry meterRegistry
    ) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    public void record(AuditEvent event) {
        if (event == null || event.eventType() == null) {
            return;
        }
        try {
            String detailsJson = serializeDetails(event.details());
            mapper.insert(
                    event.eventType().name(),
                    event.userId(),
                    boundedRemoteAddress(event.remoteAddress()),
                    boundedRequestId(MDC.get("requestId")),
                    event.success(),
                    detailsJson
            );
            increment(event.eventType(), "persisted");
        } catch (JsonProcessingException exception) {
            increment(event.eventType(), "serialization_failed");
            log.atWarn()
                    .addKeyValue("event", "audit_serialization_failed")
                    .addKeyValue("event_type", event.eventType().name())
                    .log("Audit details serialization failed");
        } catch (RuntimeException exception) {
            increment(event.eventType(), "persistence_failed");
            log.atError()
                    .addKeyValue("event", "audit_persistence_failed")
                    .addKeyValue("event_type", event.eventType().name())
                    .log("Audit event persistence failed");
        }
    }

    private String serializeDetails(Map<String, Object> details) throws JsonProcessingException {
        if (details == null || details.isEmpty()) return null;
        Object sanitized = sanitize(details, 0);
        if (!(sanitized instanceof Map<?, ?> map) || map.isEmpty()) return null;
        String serialized = objectMapper.writeValueAsString(sanitized);
        if (serialized.length() <= MAX_DETAILS_LENGTH) return serialized;
        return objectMapper.writeValueAsString(Map.of("outcome", "DETAILS_OMITTED"));
    }

    private Object sanitize(Object value, int depth) {
        if (value == null || depth > MAX_DEPTH) return null;
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (result.size() >= MAX_COLLECTION_SIZE || !(entry.getKey() instanceof String key)
                        || !ALLOWED_DETAIL_KEYS.contains(key)) continue;
                Object sanitized = sanitize(entry.getValue(), depth + 1);
                if (sanitized != null) result.put(key, sanitized);
            }
            return result;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> result = new ArrayList<>();
            for (Object item : iterable) {
                if (result.size() >= MAX_COLLECTION_SIZE) break;
                Object sanitized = sanitize(item, depth + 1);
                if (sanitized != null) result.add(sanitized);
            }
            return result;
        }
        if (value.getClass().isArray()) {
            List<Object> result = new ArrayList<>();
            for (int index = 0; index < Math.min(Array.getLength(value), MAX_COLLECTION_SIZE); index++) {
                Object sanitized = sanitize(Array.get(value, index), depth + 1);
                if (sanitized != null) result.add(sanitized);
            }
            return result;
        }
        if (value instanceof String text) {
            if (text.chars().anyMatch(character -> Character.isISOControl(character))) return null;
            return text.substring(0, Math.min(text.length(), MAX_STRING_LENGTH));
        }
        if (value instanceof Number || value instanceof Boolean) return value;
        if (value instanceof Enum<?> enumeration) return enumeration.name();
        return null;
    }

    private String boundedRemoteAddress(String remoteAddress) {
        if (remoteAddress == null || remoteAddress.length() > 45
                || !remoteAddress.matches("[0-9A-Fa-f:.]+")) return null;
        return remoteAddress;
    }

    private String boundedRequestId(String requestId) {
        if (requestId == null || !requestId.matches("[A-Za-z0-9._:-]{1,64}")) return null;
        return requestId;
    }

    private void increment(AuditEventType type, String outcome) {
        meterRegistry.counter("hbti.audit.events",
                "event_type", type.name(), "outcome", outcome).increment();
    }
}

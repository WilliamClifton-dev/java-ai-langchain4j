package com.atguigu.java.ai.langchain4j.assessment;

import com.atguigu.java.ai.langchain4j.infrastructure.redis.EphemeralStateStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

@Primary
@Repository
public class CachedHbtiDefinitionCatalog implements HbtiDefinitionCatalog {
    private static final Duration TTL = Duration.ofHours(1);
    private static final String KEY_PREFIX = "cache:hbti-definition:v1:";

    private final HbtiDefinitionRepository source;
    private final EphemeralStateStore store;
    private final ObjectMapper objectMapper;

    public CachedHbtiDefinitionCatalog(
            HbtiDefinitionRepository source,
            EphemeralStateStore store,
            ObjectMapper objectMapper
    ) {
        this.source = source;
        this.store = store;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<HbtiDefinition> findPublished(String assessmentKey, String version) {
        if (assessmentKey == null || assessmentKey.isBlank() || version == null || version.isBlank()) {
            return Optional.empty();
        }
        String key = KEY_PREFIX + sha256(assessmentKey + "\0" + version);
        Optional<HbtiDefinition> cached = read(key);
        if (cached.isPresent()) return cached;

        Optional<HbtiDefinition> loaded = source.findPublished(assessmentKey, version);
        loaded.ifPresent(definition -> write(key, definition));
        return loaded;
    }

    private Optional<HbtiDefinition> read(String key) {
        try {
            Optional<String> value = store.get(key);
            if (value.isEmpty()) return Optional.empty();
            return Optional.of(objectMapper.readValue(value.get(), HbtiDefinition.class));
        } catch (RuntimeException | JsonProcessingException exception) {
            bestEffortDelete(key);
            return Optional.empty();
        }
    }

    private void write(String key, HbtiDefinition definition) {
        try {
            store.put(key, objectMapper.writeValueAsString(definition), TTL);
        } catch (RuntimeException | JsonProcessingException ignored) {
            // Cache population is best effort; MySQL already supplied the authoritative value.
        }
    }

    private void bestEffortDelete(String key) {
        try {
            store.delete(key);
        } catch (RuntimeException ignored) {
            // A malformed or unavailable cache must never block the source-of-truth read.
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}

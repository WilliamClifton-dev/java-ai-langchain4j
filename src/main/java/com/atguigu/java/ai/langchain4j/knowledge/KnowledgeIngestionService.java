package com.atguigu.java.ai.langchain4j.knowledge;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class KnowledgeIngestionService {
    private static final int MAX_CONTENT_CHARS = 100_000;
    private static final int MAX_CHUNK_CHARS = 1_000;

    private final KnowledgeMapper mapper;
    private final Clock clock;

    public KnowledgeIngestionService(KnowledgeMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public KnowledgeDocumentVersion ingest(KnowledgeIngestionCommand command) {
        validate(command);
        String normalizedContent = command.content().strip().replace("\r\n", "\n");
        String contentHash = sha256(normalizedContent);
        Instant now = now();
        KnowledgeDocument document = mapper.findDocument(command.sourceKey()).orElseGet(() -> {
            KnowledgeDocument created = new KnowledgeDocument(
                    UUID.randomUUID().toString(), command.sourceKey(), now
            );
            mapper.insertDocument(created);
            return created;
        });
        mapper.lockDocument(document.id()).orElseThrow(() ->
                new IllegalStateException("Knowledge document lock failed"));
        Optional<KnowledgeDocumentVersion> replay = mapper.findByContent(document.id(), contentHash);
        if (replay.isPresent()) return replay.get();

        KnowledgeDocumentVersion version = new KnowledgeDocumentVersion(
                UUID.randomUUID().toString(), document.id(), mapper.nextVersion(document.id()),
                KnowledgeStatus.DRAFT, command.title().strip(), command.sourceUrl().strip(),
                command.publisher().strip(), command.locale().strip(), contentHash,
                command.reviewer().strip(), command.retrievedAt(), now, null, null
        );
        mapper.insertVersion(version);
        List<String> contentChunks = splitContent(normalizedContent);
        List<KnowledgeChunk> chunks = new ArrayList<>(contentChunks.size());
        for (int index = 0; index < contentChunks.size(); index++) {
            int ordinal = index + 1;
            String content = contentChunks.get(index);
            chunks.add(new KnowledgeChunk(UUID.randomUUID().toString(), version.id(), ordinal,
                    content, sha256(ordinal + "|" + content), now));
        }
        mapper.insertChunks(chunks);
        return version;
    }

    @Transactional
    public KnowledgeDocumentVersion publish(String versionId) {
        KnowledgeDocumentVersion version = mapper.findVersion(versionId).orElseThrow(() ->
                new InvalidKnowledgeDocumentException("Knowledge version was not found"));
        mapper.lockDocument(version.documentId()).orElseThrow(() ->
                new IllegalStateException("Knowledge document lock failed"));
        if (version.status() == KnowledgeStatus.PUBLISHED) return version;
        if (version.status() != KnowledgeStatus.DRAFT) {
            throw new InvalidKnowledgeDocumentException("Retired knowledge cannot be published");
        }
        Instant now = now();
        mapper.retirePublished(version.documentId(), version.id(), now);
        if (mapper.publish(version.id(), version.documentId(), now) != 1) {
            throw new IllegalStateException("Knowledge publication conflict");
        }
        return mapper.findVersion(version.id()).orElseThrow();
    }

    @Transactional(readOnly = true)
    public Optional<KnowledgeDocumentVersion> get(String versionId) {
        if (versionId == null || versionId.isBlank()) return Optional.empty();
        return mapper.findVersion(versionId);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeChunk> chunks(String versionId) {
        if (versionId == null || versionId.isBlank()) return List.of();
        return mapper.chunks(versionId);
    }

    private List<String> splitContent(String content) {
        List<String> result = new ArrayList<>();
        for (String paragraph : content.split("\\n\\s*\\n")) {
            String value = paragraph.strip();
            for (int start = 0; start < value.length(); start += MAX_CHUNK_CHARS) {
                result.add(value.substring(start, Math.min(value.length(), start + MAX_CHUNK_CHARS)));
            }
        }
        if (result.isEmpty()) throw new InvalidKnowledgeDocumentException("Content is empty");
        return result;
    }

    private void validate(KnowledgeIngestionCommand value) {
        if (value == null || blankOrLong(value.sourceKey(), 128) || blankOrLong(value.title(), 300)
                || blankOrLong(value.sourceUrl(), 1000) || blankOrLong(value.publisher(), 200)
                || blankOrLong(value.locale(), 16) || blankOrLong(value.reviewer(), 200)
                || value.retrievedAt() == null || value.content() == null
                || value.content().isBlank() || value.content().length() > MAX_CONTENT_CHARS) {
            throw new InvalidKnowledgeDocumentException("Knowledge document is invalid");
        }
        URI uri;
        try {
            uri = URI.create(value.sourceUrl());
        } catch (IllegalArgumentException exception) {
            throw new InvalidKnowledgeDocumentException("Knowledge source URL is invalid");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new InvalidKnowledgeDocumentException("Knowledge source URL must use HTTPS");
        }
    }

    private boolean blankOrLong(String value, int max) {
        return value == null || value.isBlank() || value.length() > max;
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private Instant now() { return clock.instant().truncatedTo(ChronoUnit.MICROS); }
}

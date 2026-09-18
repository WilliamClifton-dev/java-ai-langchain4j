package com.atguigu.java.ai.langchain4j.knowledge;

import java.time.Instant;

public record KnowledgeDocumentVersion(
        String id, String documentId, int versionNo, KnowledgeStatus status, String title,
        String sourceUrl, String publisher, String locale, String contentHash, String reviewer,
        Instant retrievedAt, Instant createdAt, Instant publishedAt, Instant retiredAt
) { }

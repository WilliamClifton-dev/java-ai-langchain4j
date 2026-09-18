package com.atguigu.java.ai.langchain4j.knowledge;

import java.time.Instant;

public record KnowledgeCitation(
        String sourceKey, String title, String sourceUrl, String publisher, String locale,
        int versionNo, String contentHash, Instant retrievedAt
) { }

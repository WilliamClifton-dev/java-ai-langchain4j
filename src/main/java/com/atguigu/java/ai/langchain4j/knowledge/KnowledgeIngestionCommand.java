package com.atguigu.java.ai.langchain4j.knowledge;

import java.time.Instant;

public record KnowledgeIngestionCommand(
        String sourceKey, String title, String sourceUrl, String publisher, String locale,
        String reviewer, Instant retrievedAt, String content
) { }

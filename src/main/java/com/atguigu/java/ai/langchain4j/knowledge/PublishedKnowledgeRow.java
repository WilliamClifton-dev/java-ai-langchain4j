package com.atguigu.java.ai.langchain4j.knowledge;

import java.time.Instant;

record PublishedKnowledgeRow(
        String sourceKey, String title, String sourceUrl, String publisher, String locale,
        int versionNo, String versionContentHash, Instant retrievedAt, int ordinal, String content
) { }

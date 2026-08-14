package com.atguigu.java.ai.langchain4j.knowledge;

import java.time.Instant;

public record KnowledgeChunk(String id, String versionId, int ordinal, String content,
                             String contentHash, Instant createdAt) { }

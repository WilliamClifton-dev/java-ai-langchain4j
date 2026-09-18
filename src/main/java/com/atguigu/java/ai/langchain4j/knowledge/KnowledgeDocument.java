package com.atguigu.java.ai.langchain4j.knowledge;

import java.time.Instant;

record KnowledgeDocument(String id, String sourceKey, Instant createdAt) { }

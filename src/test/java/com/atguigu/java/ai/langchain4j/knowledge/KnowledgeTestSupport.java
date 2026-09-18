package com.atguigu.java.ai.langchain4j.knowledge;

import com.atguigu.java.ai.langchain4j.config.TimeConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

@ActiveProfiles("test")
@Import({KnowledgeIngestionService.class, ReviewedKnowledgeRetriever.class, TimeConfig.class})
abstract class KnowledgeTestSupport {
    @Autowired KnowledgeIngestionService ingestion;
    @Autowired ReviewedKnowledgeRetriever retriever;

    protected void publish(String sourceKey, String title, String content) {
        ingest(sourceKey, title, content, true);
    }

    protected void ingest(String sourceKey, String title, String content, boolean publish) {
        KnowledgeDocumentVersion version = ingestion.ingest(new KnowledgeIngestionCommand(
                sourceKey, title, "https://example.org/" + sourceKey,
                "Example Institute", "zh-CN", "reviewer",
                Instant.parse("2026-08-01T00:00:00Z"), content
        ));
        if (publish) ingestion.publish(version.id());
    }
}

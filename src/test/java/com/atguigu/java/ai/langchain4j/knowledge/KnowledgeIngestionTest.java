package com.atguigu.java.ai.langchain4j.knowledge;

import com.atguigu.java.ai.langchain4j.config.TimeConfig;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({KnowledgeIngestionService.class, TimeConfig.class})
class KnowledgeIngestionTest {
    @Autowired KnowledgeIngestionService service;

    @Test
    void replaysIdenticalContentAndVersionsChangedReviewedContent() {
        KnowledgeIngestionCommand first = command("每天累计活动，逐步增加训练量。", "reviewer-a");
        KnowledgeDocumentVersion v1 = service.ingest(first);

        assertThat(service.ingest(first)).isEqualTo(v1);
        assertThat(v1.versionNo()).isEqualTo(1);
        assertThat(v1.status()).isEqualTo(KnowledgeStatus.DRAFT);
        assertThat(service.publish(v1.id()).status()).isEqualTo(KnowledgeStatus.PUBLISHED);

        KnowledgeDocumentVersion v2 = service.ingest(command(
                "每天累计活动，逐步增加训练量。\n\n每周根据执行情况复盘。", "reviewer-b"));
        service.publish(v2.id());

        assertThat(v2.versionNo()).isEqualTo(2);
        assertThat(service.get(v1.id())).get().extracting(KnowledgeDocumentVersion::status)
                .isEqualTo(KnowledgeStatus.RETIRED);
        assertThat(service.get(v2.id())).get().extracting(KnowledgeDocumentVersion::status)
                .isEqualTo(KnowledgeStatus.PUBLISHED);
        assertThat(service.chunks(v2.id())).extracting(KnowledgeChunk::ordinal)
                .containsExactly(1, 2);
    }

    private KnowledgeIngestionCommand command(String content, String reviewer) {
        return new KnowledgeIngestionCommand(
                "activity-guide", "活动与训练指南", "https://example.org/activity",
                "Example Health Institute", "zh-CN", reviewer,
                Instant.parse("2026-08-01T00:00:00Z"), content
        );
    }
}

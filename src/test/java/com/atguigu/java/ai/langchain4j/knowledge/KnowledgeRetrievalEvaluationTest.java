package com.atguigu.java.ai.langchain4j.knowledge;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class KnowledgeRetrievalEvaluationTest extends KnowledgeTestSupport {
    @Test
    void meetsTheVersionedRecallCitationAndStaleExclusionFixtures() {
        publish("sleep", "睡眠记录", "固定时间记录睡眠分钟和主观睡眠质量。");
        publish("nutrition", "饮食记录", "饮食记录应包含能量和蛋白质克数。");

        assertThat(retriever.search("怎么记录蛋白质克数", "zh-CN", 2))
                .first().extracting(value -> value.citation().sourceKey()).isEqualTo("nutrition");
        assertThat(retriever.search("如何记录睡眠分钟", "zh-CN", 2))
                .first().extracting(value -> value.citation().sourceKey()).isEqualTo("sleep");

        publish("sleep", "睡眠记录新版", "每天记录恢复感受并保持一致作息。");

        assertThat(retriever.search("主观睡眠质量", "zh-CN", 3))
                .noneMatch(value -> value.citation().title().equals("睡眠记录"));
        assertThat(retriever.search("伪造不存在的权威来源", "zh-CN", 3)).isEmpty();
    }
}

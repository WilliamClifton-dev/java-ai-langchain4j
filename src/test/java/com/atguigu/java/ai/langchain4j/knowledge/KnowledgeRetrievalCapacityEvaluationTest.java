package com.atguigu.java.ai.langchain4j.knowledge;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.transaction.TestTransaction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:hbti_knowledge_capacity;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class KnowledgeRetrievalCapacityEvaluationTest extends KnowledgeTestSupport {

    private static final int DOCUMENTS = 5;
    private static final int CHUNKS_PER_DOCUMENT = 100;
    private static final int CONCURRENCY = 10;
    private static final int QUERIES = 40;
    private static final long P95_LIMIT_MILLIS = 2_000;

    @Test
    void recordsRecallAndLatencyAtTheFiveHundredCandidateBoundary() throws Exception {
        for (int document = 0; document < DOCUMENTS; document++) {
            List<String> paragraphs = new ArrayList<>();
            for (int chunk = 0; chunk < CHUNKS_PER_DOCUMENT; chunk++) {
                paragraphs.add("reviewed protein tracking guidance source " + document
                        + " section " + chunk + " consistency recovery nutrition activity");
            }
            publish("capacity-" + document, "Capacity source " + document,
                    String.join("\n\n", paragraphs));
        }
        TestTransaction.flagForCommit();
        TestTransaction.end();

        List<Long> durations = Collections.synchronizedList(new ArrayList<>());
        List<Callable<Boolean>> calls = new ArrayList<>();
        for (int query = 0; query < QUERIES; query++) {
            calls.add(() -> {
                Instant started = Instant.now();
                List<KnowledgePassage> results = retriever.search(
                        "reviewed protein tracking guidance", "zh-CN", 5);
                durations.add(Duration.between(started, Instant.now()).toMillis());
                return results.size() == 5 && results.stream()
                        .allMatch(result -> result.citation().sourceKey().startsWith("capacity-"));
            });
        }

        List<Boolean> recallResults;
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY);
        try {
            recallResults = executor.invokeAll(calls).stream().map(future -> {
                try {
                    return future.get();
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            }).toList();
        } finally {
            executor.shutdownNow();
        }

        List<Long> sorted = durations.stream().sorted().toList();
        long p95Millis = sorted.get((int) Math.ceil(sorted.size() * 0.95) - 1);
        assertThat(recallResults).allMatch(Boolean::booleanValue);
        assertThat(p95Millis).isLessThanOrEqualTo(P95_LIMIT_MILLIS);
        writeEvidence(p95Millis, sorted.get(sorted.size() - 1));
    }

    private void writeEvidence(long p95Millis, long maxMillis) throws IOException {
        Path output = Path.of("target/release-evidence/ai-safety/rag-performance.json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, """
                {
                  "schemaVersion": "1.0.0",
                  "status": "PASS",
                  "candidateChunks": %d,
                  "concurrency": %d,
                  "queries": %d,
                  "p95Millis": %d,
                  "maxMillis": %d,
                  "p95LimitMillis": %d,
                  "allQueriesRetrievedFiveCitedPassages": true
                }
                """.formatted(DOCUMENTS * CHUNKS_PER_DOCUMENT, CONCURRENCY, QUERIES,
                p95Millis, maxMillis, P95_LIMIT_MILLIS));
    }
}

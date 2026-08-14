package com.atguigu.java.ai.langchain4j.tracking;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DailySummaryAggregationTest extends TrackingIntegrationTestSupport {

    @Test
    void aggregatesMultipleTrainingSessionsInStableOrder() {
        String userId = user("tracking-summary");
        LocalDate date = LocalDate.now();
        service.recordTraining(userId, "training-1", new TrainingCommand(
                date, TrainingType.STRENGTH, 50, TrainingIntensity.HIGH
        ));
        service.recordTraining(userId, "training-2", new TrainingCommand(
                date, TrainingType.MOBILITY, 20, TrainingIntensity.LOW
        ));

        DailySummary summary = service.summary(userId, date);

        assertThat(summary.trainingSessions()).extracting(TrainingLog::trainingType)
                .containsExactly(TrainingType.STRENGTH, TrainingType.MOBILITY);
        assertThat(summary.trainingMinutes()).isEqualTo(70);
    }
}

package com.atguigu.java.ai.langchain4j.tracking;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class WeeklyReviewPersistenceTest extends WeeklyReviewIntegrationTestSupport {

    @Test
    void replaysUnchangedInputsAndVersionsLateFactsWithoutOverwritingHistory() {
        String userId = activeUser("weekly-persistence");
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        for (int day : new int[]{0, 2, 4, 6}) {
            tracking.recordMetric(userId, "metric-" + day, new DailyMetricCommand(
                    start.plusDays(day), new BigDecimal("70.0").subtract(
                    new BigDecimal("0.1").multiply(BigDecimal.valueOf(day))
            ), 8000 + day * 100, 30, 450, 4));
        }
        for (int day = 0; day < 4; day++) {
            tracking.recordNutrition(userId, "nutrition-" + day, new NutritionCommand(
                    start.plusDays(day), 1700, new BigDecimal("120"),
                    new BigDecimal("200"), new BigDecimal("60")
            ));
        }

        WeeklyReviewWrite created = reviews.generate(userId, end);
        WeeklyReviewWrite replayed = reviews.generate(userId, end);

        assertThat(created.replayed()).isFalse();
        assertThat(created.review().versionNo()).isEqualTo(1);
        assertThat(replayed.replayed()).isTrue();
        assertThat(replayed.review()).isEqualTo(created.review());

        tracking.recordTraining(userId, "late-training", new TrainingCommand(
                end, TrainingType.STRENGTH, 60, TrainingIntensity.MODERATE
        ));
        WeeklyReviewWrite revised = reviews.generate(userId, end);

        assertThat(revised.replayed()).isFalse();
        assertThat(revised.review().versionNo()).isEqualTo(2);
        assertThat(revised.review().totalTrainingMinutes()).isEqualTo(60);
        assertThat(reviews.get(userId, created.review().id())).contains(created.review());
        assertThat(reviews.get(activeUser("weekly-other"), created.review().id())).isEmpty();
    }
}

package com.atguigu.java.ai.langchain4j.tracking;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DailyTrackingIdempotencyTest extends TrackingIntegrationTestSupport {

    @Test
    void replaysTheSamePayloadAndRejectsKeyReuseWithDifferentFacts() {
        String userId = user("tracking-idempotency");
        LocalDate date = LocalDate.now();
        DailyMetricCommand command = new DailyMetricCommand(
                date, new BigDecimal("70.20"), 8000, 45, 450, 4
        );

        TrackingWrite<DailyMetric> created = service.recordMetric(userId, "metric-key", command);
        TrackingWrite<DailyMetric> replayed = service.recordMetric(userId, "metric-key", command);

        assertThat(created.replayed()).isFalse();
        assertThat(replayed.replayed()).isTrue();
        assertThat(replayed.record()).isEqualTo(created.record());
        assertThatThrownBy(() -> service.recordMetric(
                userId, "metric-key",
                new DailyMetricCommand(date, new BigDecimal("71"), 8000, 45, 450, 4)
        )).isInstanceOf(TrackingIdempotencyConflictException.class);
    }

    @Test
    void acceptsActivityWithoutWeightAndRejectsFutureLocalDates() {
        String userId = user("tracking-optional");
        LocalDate today = LocalDate.now();

        TrackingWrite<DailyMetric> activity = service.recordMetric(
                userId, "activity-only",
                new DailyMetricCommand(today, null, 6000, 30, null, null)
        );

        assertThat(activity.record().weightKg()).isNull();
        assertThat(activity.record().steps()).isEqualTo(6000);
        assertThatThrownBy(() -> service.recordMetric(
                userId, "future-metric",
                new DailyMetricCommand(today.plusDays(1), null, 1, null, null, null)
        )).isInstanceOf(InvalidTrackingRequestException.class);
    }
}

package com.atguigu.java.ai.langchain4j.coach.tool;

import com.atguigu.java.ai.langchain4j.coach.streaming.CoachMetrics;
import com.atguigu.java.ai.langchain4j.planning.WeightPlanService;
import com.atguigu.java.ai.langchain4j.tracking.DailyTrackingService;
import com.atguigu.java.ai.langchain4j.tracking.WeeklyReviewService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CoachToolMetricsTest {

    @Test
    void recordsFixedToolNameAndBoundedOutcome() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CoachTools tools = new CoachTools(
                new CoachToolContext(), mock(WeightPlanService.class),
                mock(DailyTrackingService.class), mock(WeeklyReviewService.class),
                new CoachMetrics(registry));

        CoachToolResult<?> result = tools.getDailySummary(LocalDate.of(2026, 8, 15).toString());

        assertThat(result.code()).isEqualTo("TOOL_UNAUTHORIZED");
        assertThat(registry.get("hbti.coach.tool.calls")
                .tag("tool", "get_daily_summary")
                .tag("outcome", "TOOL_UNAUTHORIZED")
                .counter().count()).isEqualTo(1);
    }
}

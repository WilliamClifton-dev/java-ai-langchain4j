package com.atguigu.java.ai.langchain4j.coach.tool;

import com.atguigu.java.ai.langchain4j.planning.WeightPlanService;
import com.atguigu.java.ai.langchain4j.tracking.DailyTrackingService;
import com.atguigu.java.ai.langchain4j.tracking.TrainingCommand;
import com.atguigu.java.ai.langchain4j.tracking.WeeklyReviewService;
import com.atguigu.java.ai.langchain4j.coach.streaming.CoachMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class CoachToolRollbackTest {
    @Test
    void neverReportsSuccessWhenTheApplicationWriteThrows() {
        DailyTrackingService tracking = mock(DailyTrackingService.class);
        doThrow(new IllegalStateException("transaction rolled back")).when(tracking)
                .recordTraining(eq("owner-1"), any(), any(TrainingCommand.class));
        CoachToolContext context = new CoachToolContext();
        CoachTools tools = new CoachTools(context, mock(WeightPlanService.class), tracking,
                mock(WeeklyReviewService.class), new CoachMetrics(new SimpleMeterRegistry()));

        CoachToolResult<?> result = context.callAs("owner-1", "conversation-1", () ->
                tools.recordTraining(LocalDate.now().toString(), "STRENGTH", 45, "MODERATE"));

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("TOOL_WRITE_FAILED");
        assertThat(result.data()).isNull();
    }
}

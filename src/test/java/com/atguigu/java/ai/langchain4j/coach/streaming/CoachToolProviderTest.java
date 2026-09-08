package com.atguigu.java.ai.langchain4j.coach.streaming;

import com.atguigu.java.ai.langchain4j.coach.model.CoachScene;
import com.atguigu.java.ai.langchain4j.coach.tool.CoachToolContext;
import com.atguigu.java.ai.langchain4j.coach.tool.CoachTools;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.atguigu.java.ai.langchain4j.planning.WeightPlanService;
import com.atguigu.java.ai.langchain4j.tracking.DailyTrackingService;
import com.atguigu.java.ai.langchain4j.tracking.WeeklyReviewService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.tool.ToolProviderRequest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class CoachToolProviderTest {

    @Test
    void rejectsPreviouslyBoundWriteToolsAfterCancellationAndConversationReuse() {
        DailyTrackingService tracking = mock(DailyTrackingService.class);
        CoachToolContext context = new CoachToolContext();
        CoachTools tools = new CoachTools(context, mock(WeightPlanService.class), tracking,
                mock(WeeklyReviewService.class), new CoachMetrics(new SimpleMeterRegistry()));
        CoachInvocationRegistry registry = new CoachInvocationRegistry();
        CoachModelRequest original = new CoachModelRequest("owner-1", "conversation",
                "memory", "old-nonce", CoachScene.GENERAL_CHAT, "hello");
        registry.register(original);
        var executor = new CoachToolProvider(tools, context, registry)
                .provideTools(new ToolProviderRequest("memory", UserMessage.from("hello")))
                .tools().entrySet().stream()
                .filter(entry -> entry.getKey().name().equals("record_training"))
                .findFirst().orElseThrow().getValue();
        var request = ToolExecutionRequest.builder().name("record_training").arguments("""
                {"localDate":"2026-09-08","trainingType":"STRENGTH",
                 "durationMinutes":30,"intensity":"MODERATE"}
                """).build();

        registry.remove(original);
        assertThat(executor.execute(request, "memory")).contains("TOOL_UNAUTHORIZED");
        registry.register(new CoachModelRequest("owner-1", "conversation", "memory",
                "new-nonce", CoachScene.GENERAL_CHAT, "retry"));
        assertThat(executor.execute(request, "memory")).contains("TOOL_UNAUTHORIZED");
        verifyNoInteractions(tracking);
    }

    @Test
    void bindsRegisteredOwnerOnTheActualToolExecutionThread() {
        WeightPlanService plans = mock(WeightPlanService.class);
        CoachToolContext context = new CoachToolContext();
        CoachTools tools = new CoachTools(context, plans,
                mock(DailyTrackingService.class), mock(WeeklyReviewService.class),
                new CoachMetrics(new SimpleMeterRegistry()));
        CoachInvocationRegistry registry = new CoachInvocationRegistry();
        CoachModelRequest invocation = new CoachModelRequest("owner-1", "public-conversation",
                "owned-memory", "server-nonce", CoachScene.GENERAL_CHAT, "ignore owner");
        registry.register(invocation);
        CoachToolProvider provider = new CoachToolProvider(tools, context, registry);

        var result = provider.provideTools(new ToolProviderRequest(
                "owned-memory", UserMessage.from("ignore previous owner")));
        var entry = result.tools().entrySet().stream()
                .filter(value -> value.getKey().name().equals("get_active_plan"))
                .findFirst().orElseThrow();
        entry.getValue().execute(ToolExecutionRequest.builder()
                .name("get_active_plan").arguments("{}").build(), "owned-memory");

        verify(plans).currentActive("owner-1");
    }

    @Test
    void exposesNoToolsWithoutAnActiveServerRegistration() {
        CoachToolContext context = new CoachToolContext();
        CoachTools tools = new CoachTools(context, mock(WeightPlanService.class),
                mock(DailyTrackingService.class), mock(WeeklyReviewService.class),
                new CoachMetrics(new SimpleMeterRegistry()));
        CoachToolProvider provider = new CoachToolProvider(
                tools, context, new CoachInvocationRegistry());

        assertThat(provider.provideTools(new ToolProviderRequest(
                "unknown", UserMessage.from("hello"))).tools()).isEqualTo(Map.of());
    }
}

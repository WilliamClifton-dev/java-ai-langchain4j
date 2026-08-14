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

class CoachToolProviderTest {

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

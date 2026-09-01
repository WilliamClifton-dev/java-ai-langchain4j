package com.atguigu.java.ai.langchain4j.coach.streaming;

import com.atguigu.java.ai.langchain4j.coach.tool.CoachToolContext;
import com.atguigu.java.ai.langchain4j.coach.tool.CoachTools;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CoachToolProvider implements ToolProvider {
    private final CoachToolContext context;
    private final CoachInvocationRegistry registry;
    private final Map<ToolSpecification, ToolExecutor> executors;

    public CoachToolProvider(CoachTools tools, CoachToolContext context,
                             CoachInvocationRegistry registry) {
        if (tools == null || context == null || registry == null) {
            throw new IllegalArgumentException("Coach tool provider is invalid");
        }
        this.context = context;
        this.registry = registry;
        this.executors = executors(tools);
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        if (request == null) return ToolProviderResult.builder().build();
        return registry.find(request.chatMemoryId()).map(invocation -> {
            Map<ToolSpecification, ToolExecutor> bound = new LinkedHashMap<>();
            executors.forEach((specification, delegate) -> bound.put(specification,
                    (toolRequest, memoryId) -> context.callAs(
                            invocation.userId(), invocation.conversationId(),
                            invocation.requestNonce(),
                            () -> delegate.execute(toolRequest, memoryId))));
            return ToolProviderResult.builder().addAll(bound).build();
        }).orElseGet(() -> ToolProviderResult.builder().build());
    }

    private Map<ToolSpecification, ToolExecutor> executors(CoachTools tools) {
        Map<ToolSpecification, ToolExecutor> result = new LinkedHashMap<>();
        for (Method method : CoachTools.class.getMethods()) {
            if (method.getAnnotation(Tool.class) == null) continue;
            result.put(ToolSpecifications.toolSpecificationFrom(method),
                    new DefaultToolExecutor(tools, method));
        }
        return Map.copyOf(result);
    }

}

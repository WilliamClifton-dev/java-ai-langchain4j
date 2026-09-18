package com.atguigu.java.ai.langchain4j.coach.streaming;

import com.atguigu.java.ai.langchain4j.assistant.HbtiCoachStreamingAgent;
import com.atguigu.java.ai.langchain4j.coach.prompt.ScenePromptRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LangChain4jCoachStreamingModel implements CoachStreamingModel {
    private final HbtiCoachStreamingAgent agent;
    private final CoachInvocationRegistry registry;
    private final ScenePromptRepository prompts;
    private final Clock clock;

    public LangChain4jCoachStreamingModel(HbtiCoachStreamingAgent agent,
                                         CoachInvocationRegistry registry,
                                         ScenePromptRepository prompts, Clock clock) {
        this.agent = agent;
        this.registry = registry;
        this.prompts = prompts;
        this.clock = clock;
    }

    @Override
    public CoachStreamHandle start(CoachModelRequest request, CoachModelListener listener) {
        registry.register(request);
        AtomicBoolean terminal = new AtomicBoolean();
        try {
            agent.chat(request.memoryId(), LocalDate.now(clock).toString(),
                            prompts.get(request.scene()), request.message())
                    .onPartialResponse(token -> {
                        if (!terminal.get()) listener.onToken(token);
                    })
                    .onCompleteResponse(response -> {
                        if (terminal.compareAndSet(false, true)) {
                            registry.remove(request);
                            listener.onComplete();
                        }
                    })
                    .onError(failure -> {
                        if (terminal.compareAndSet(false, true)) {
                            registry.remove(request);
                            listener.onError(failure);
                        }
                    })
                    .start();
        } catch (RuntimeException failure) {
            terminal.set(true);
            registry.remove(request);
            throw failure;
        }
        return () -> {
            if (terminal.compareAndSet(false, true)) registry.remove(request);
        };
    }
}

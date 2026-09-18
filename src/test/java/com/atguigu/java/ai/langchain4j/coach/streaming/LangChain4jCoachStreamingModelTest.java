package com.atguigu.java.ai.langchain4j.coach.streaming;

import com.atguigu.java.ai.langchain4j.assistant.HbtiCoachStreamingAgent;
import com.atguigu.java.ai.langchain4j.coach.model.CoachScene;
import com.atguigu.java.ai.langchain4j.coach.prompt.ScenePromptRepository;
import dev.langchain4j.service.TokenStream;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LangChain4jCoachStreamingModelTest {

    @Test
    void registersExplicitInvocationUntilCompletionAndSuppressesAfterCancellation() {
        HbtiCoachStreamingAgent agent = mock(HbtiCoachStreamingAgent.class);
        TokenStream stream = mock(TokenStream.class);
        List<Consumer<String>> tokenCallbacks = new ArrayList<>();
        List<Consumer<Throwable>> errorCallbacks = new ArrayList<>();
        List<Consumer<dev.langchain4j.model.chat.response.ChatResponse>> completeCallbacks =
                new ArrayList<>();
        when(stream.onPartialResponse(any())).thenAnswer(call -> {
            tokenCallbacks.add(call.getArgument(0)); return stream;
        });
        when(stream.onError(any())).thenAnswer(call -> {
            errorCallbacks.add(call.getArgument(0)); return stream;
        });
        when(stream.onCompleteResponse(any())).thenAnswer(call -> {
            completeCallbacks.add(call.getArgument(0)); return stream;
        });
        when(agent.chat(eq("owned-memory"), eq("2026-08-15"), any(), eq("hello")))
                .thenReturn(stream);
        CoachInvocationRegistry registry = new CoachInvocationRegistry();
        LangChain4jCoachStreamingModel model = new LangChain4jCoachStreamingModel(agent, registry,
                new ScenePromptRepository(), Clock.fixed(
                Instant.parse("2026-08-15T02:00:00Z"), ZoneOffset.UTC));
        RecordingListener listener = new RecordingListener();
        CoachModelRequest request = new CoachModelRequest("owner-1", "conversation-1",
                "owned-memory", "nonce", CoachScene.GENERAL_CHAT, "hello");

        CoachStreamHandle handle = model.start(request, listener);
        assertThat(registry.find("owned-memory")).contains(request);
        tokenCallbacks.get(0).accept("first");
        handle.cancel();
        tokenCallbacks.get(0).accept("late");

        assertThat(listener.events).containsExactly("token:first");
        assertThat(registry.find("owned-memory")).isEmpty();
        verify(stream).start();
    }

    private static final class RecordingListener implements CoachModelListener {
        private final List<String> events = new ArrayList<>();

        @Override public void onToken(String text) { events.add("token:" + text); }
        @Override public void onComplete() { events.add("complete"); }
        @Override public void onError(Throwable failure) { events.add("error"); }
    }
}

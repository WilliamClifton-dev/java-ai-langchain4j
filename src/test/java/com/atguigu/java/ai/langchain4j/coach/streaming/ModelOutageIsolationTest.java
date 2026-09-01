package com.atguigu.java.ai.langchain4j.coach.streaming;

import com.atguigu.java.ai.langchain4j.coach.dto.CoachChatCommand;
import com.atguigu.java.ai.langchain4j.coach.model.CoachScene;
import com.atguigu.java.ai.langchain4j.planning.HealthCalculationInput;
import com.atguigu.java.ai.langchain4j.planning.HealthCalculator;
import com.atguigu.java.ai.langchain4j.profile.ActivityLevel;
import com.atguigu.java.ai.langchain4j.profile.CalculationSex;
import com.atguigu.java.ai.langchain4j.store.CoachConversationOwnershipService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.profiles.active=test")
class ModelOutageIsolationTest {

    @MockBean(name = "chatModel")
    private ChatLanguageModel chatModel;

    // This test isolates provider outage behavior; ownership is covered by dedicated tests.
    @MockBean
    private CoachConversationOwnershipService ownership;

    @Autowired
    private CoachStreamingService streamingService;

    @Autowired
    private HealthCalculator calculator;

    @Test
    void deterministicCalculationsRemainAvailableWhenTheModelPortFails() {
        RecordingSink sink = new RecordingSink();

        streamingService.open(new CoachChatCommand("user-1", "conversation-1",
                CoachScene.GENERAL_CHAT, "hello"), sink);
        var calculation = calculator.calculate(new HealthCalculationInput(
                30, CalculationSex.MALE, 180, 80, ActivityLevel.MODERATE));

        assertThat(sink.events).containsExactly("metadata", "error:MODEL_UNAVAILABLE");
        assertThat(calculation.bmrKcalPerDay()).isEqualTo(1780);
        assertThat(calculation.tdeeKcalPerDay()).isEqualTo(2759);
    }

    private static final class RecordingSink implements CoachEventSink {
        private final List<String> events = new ArrayList<>();

        @Override public void metadata(String id, CoachScene scene) { events.add("metadata"); }
        @Override public void token(long sequence, String text) { events.add("token"); }
        @Override public void completion(String id) { events.add("completion"); }
        @Override public void error(String code, boolean retryable) {
            events.add("error:" + code);
        }
    }
}

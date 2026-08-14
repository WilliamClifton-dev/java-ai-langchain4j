package com.atguigu.java.ai.langchain4j.common.observability;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.encoder.JsonEncoder;
import ch.qos.logback.core.OutputStreamAppender;
import com.atguigu.java.ai.langchain4j.coach.service.CoachChatService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class StructuredLoggingConfigurationTest {

    @MockBean
    private CoachChatService coachChatService;

    @MockBean(name = "chatModel")
    private ChatLanguageModel chatModel;

    @Test
    void consoleAppenderUsesTheBuiltInJsonEncoder() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

        assertThat(root.getAppender("CONSOLE"))
                .isInstanceOfSatisfying(OutputStreamAppender.class,
                        appender -> assertThat(appender.getEncoder())
                                .isInstanceOf(JsonEncoder.class));
    }
}

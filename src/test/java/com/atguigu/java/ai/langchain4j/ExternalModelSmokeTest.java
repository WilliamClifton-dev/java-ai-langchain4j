package com.atguigu.java.ai.langchain4j;

import com.atguigu.java.ai.langchain4j.config.ChatModelConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ChatModelConfig.class)
@ActiveProfiles("minimax")
@EnabledIfEnvironmentVariable(named = "RUN_EXTERNAL_TESTS", matches = "true")
class ExternalModelSmokeTest {

    @Autowired
    private ChatLanguageModel chatModel;

    @Test
    void receivesAResponseFromTheConfiguredModel() {
        String answer = chatModel.chat("请只回答：连接成功");

        assertThat(answer).isNotBlank();
    }
}

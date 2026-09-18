package com.atguigu.java.ai.langchain4j.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OfflineChatModelConfigTest {

    @Test
    void failsLocallyWhenTheExternalModelIsDisabled() {
        ChatLanguageModel model = new ChatModelConfig().offlineChatModel();

        assertThatThrownBy(() -> model.chat("hello"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("External model is disabled");
    }
}

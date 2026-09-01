package com.atguigu.java.ai.langchain4j;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = "spring.profiles.active=test")
class ApplicationContextTest {

    @MockBean(name = "chatModel")
    private ChatLanguageModel chatModel;

    @Test
    void applicationContextLoads() {
    }
}

package com.atguigu.java.ai.langchain4j;

import ch.qos.logback.classic.spi.EventArgUtil;
import com.atguigu.java.ai.langchain4j.assistant.Assistant;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.spring.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AIServiceTest {

    @Autowired
    private Assistant assistant;

    @Test
    void testChat(){
        String result = assistant.chat("你是谁？");
        System.out.println(result);
    }

}

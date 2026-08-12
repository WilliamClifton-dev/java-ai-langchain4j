package com.atguigu.java.ai.langchain4j;

import com.atguigu.java.ai.langchain4j.assistant.Assistant;
import com.atguigu.java.ai.langchain4j.assistant.MemoryChatAssistant;
import com.atguigu.java.ai.langchain4j.assistant.SeparateChatAssistant;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class find {

    @Autowired
    private Assistant assistant;
    @Autowired
    private SeparateChatAssistant separateChatAssistant;

    @Test
    public void test() {
        String answer1 = separateChatAssistant.chat(1,"我是张三");
        System.out.println(answer1);
        String answer2 = separateChatAssistant.chat(1,"我是谁？");
        System.out.println(answer2);
        String answer3 = separateChatAssistant.chat(2,"我是谁？");
        System.out.println(answer3);
    }

}

package com.atguigu.java.ai.langchain4j;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class LLMTest {

//    @Test
//    public void testGPTDemo() {
//
//        OpenAiChatModel model = OpenAiChatModel.builder()
//                .baseUrl("http://langchain4j.dev/demo/openai/v1")
//                .apiKey("demo")
//                .modelName("gpt-4o-mini")
//                .build();
//
//        String answer = model.chat("你好");
//        System.out.println(answer);
//    }

//    @Autowired
//    private OpenAiChatModel openAiChatModel;
//
//    @Test
//    public void testSpringBoot() {
//        System.out.println("\n=== Spring注入模型测试 ===");
//        String answer = openAiChatModel.chat("1+1=?");
//        System.out.println(answer);
//    }

    @Autowired
    private ChatLanguageModel chatModel;

    @Test
    public void testChatModel() {
        String answer = chatModel.chat("你是谁？");
        System.out.println(answer);
    }
}

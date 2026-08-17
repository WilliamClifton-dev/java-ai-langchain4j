package com.atguigu.java.ai.langchain4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HbtiCoachApplication {

    public static void main(String[] args) {
        SpringApplication.run(HbtiCoachApplication.class, args);
    }
}

package com.atguigu.java.ai.langchain4j.planning;

public class PlanVersionNotFoundException extends RuntimeException {

    public PlanVersionNotFoundException() {
        super("Plan version was not found");
    }
}

package com.atguigu.java.ai.langchain4j.assessment;

public class AssessmentResultNotFoundException extends RuntimeException {

    public AssessmentResultNotFoundException() {
        super("Assessment result was not found");
    }
}

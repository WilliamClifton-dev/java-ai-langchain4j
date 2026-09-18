package com.atguigu.java.ai.langchain4j.assessment;

public class AssessmentDefinitionNotFoundException extends RuntimeException {

    public AssessmentDefinitionNotFoundException() {
        super("Published assessment definition was not found");
    }
}

package com.atguigu.java.ai.langchain4j.assessment;

public class InvalidAssessmentRequestException extends IllegalArgumentException {

    public InvalidAssessmentRequestException(String message) {
        super(message);
    }
}

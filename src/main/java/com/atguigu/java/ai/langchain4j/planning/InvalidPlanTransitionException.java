package com.atguigu.java.ai.langchain4j.planning;

public class InvalidPlanTransitionException extends RuntimeException {

    public InvalidPlanTransitionException(PlanVersionStatus expected, PlanVersionStatus actual) {
        super("Plan version must be " + expected + " but was " + actual);
    }
}

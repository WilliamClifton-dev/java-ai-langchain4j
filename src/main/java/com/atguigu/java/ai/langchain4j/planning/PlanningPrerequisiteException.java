package com.atguigu.java.ai.langchain4j.planning;

public class PlanningPrerequisiteException extends RuntimeException {

    private final PlanningPrerequisiteReason reason;

    public PlanningPrerequisiteException(PlanningPrerequisiteReason reason) {
        super("Automatic planning prerequisites are not current");
        this.reason = reason;
    }

    public PlanningPrerequisiteReason reason() {
        return reason;
    }
}

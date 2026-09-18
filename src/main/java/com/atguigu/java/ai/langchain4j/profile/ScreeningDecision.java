package com.atguigu.java.ai.langchain4j.profile;

import java.util.List;

public record ScreeningDecision(
        ScreeningStatus status,
        boolean automaticPlanningAllowed,
        List<ScreeningReason> reasonCodes
) {
    public ScreeningDecision {
        reasonCodes = List.copyOf(reasonCodes);
    }
}

package com.atguigu.java.ai.langchain4j.planning.api;

import com.atguigu.java.ai.langchain4j.planning.WeightGoal;
import jakarta.validation.constraints.NotNull;

public record WeightPlanDraftRequest(@NotNull WeightGoal goal) {
}

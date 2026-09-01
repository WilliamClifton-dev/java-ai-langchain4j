package com.atguigu.java.ai.langchain4j.coach.tool;

import dev.langchain4j.agent.tool.Tool;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CoachToolSchemaTest {
    @Test
    void exposesOnlyTheReviewedAllowlistAndNoOwnerArgument() {
        Set<Method> methods = Arrays.stream(CoachTools.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Tool.class)).collect(Collectors.toSet());

        assertThat(methods).extracting(method -> method.getAnnotation(Tool.class).name())
                .containsExactlyInAnyOrder("get_active_plan", "get_daily_summary",
                        "get_weekly_review", "record_daily_metric", "record_nutrition",
                        "record_training");
        assertThat(methods).allSatisfy(method -> assertThat(Arrays.stream(method.getParameters())
                .map(parameter -> parameter.getName().toLowerCase()).toList())
                .noneMatch(name -> name.contains("userid") || name.equals("owner")));
    }
}

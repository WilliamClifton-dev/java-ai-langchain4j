package com.atguigu.java.ai.langchain4j.coach.tool;

public record CoachToolResult<T>(boolean success, String code, T data) {
    static <T> CoachToolResult<T> success(T data) {
        return new CoachToolResult<>(true, "OK", data);
    }

    static <T> CoachToolResult<T> failure(String code) {
        return new CoachToolResult<>(false, code, null);
    }
}

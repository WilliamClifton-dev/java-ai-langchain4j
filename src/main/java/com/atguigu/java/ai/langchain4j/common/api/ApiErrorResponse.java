package com.atguigu.java.ai.langchain4j.common.api;

import java.util.Map;

public record ApiErrorResponse(ApiError error) {

    public record ApiError(
            String code,
            String message,
            Map<String, String> details
    ) {
    }
}

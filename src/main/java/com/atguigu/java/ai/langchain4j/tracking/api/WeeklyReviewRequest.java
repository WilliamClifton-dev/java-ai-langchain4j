package com.atguigu.java.ai.langchain4j.tracking.api;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record WeeklyReviewRequest(@NotNull LocalDate windowEnd) { }

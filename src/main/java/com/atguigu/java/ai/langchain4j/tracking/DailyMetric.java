package com.atguigu.java.ai.langchain4j.tracking;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record DailyMetric(String id, LocalDate localDate, BigDecimal weightKg, Integer steps,
                          Integer activityMinutes, Integer sleepMinutes, Integer sleepQuality,
                          Instant createdAt) { }

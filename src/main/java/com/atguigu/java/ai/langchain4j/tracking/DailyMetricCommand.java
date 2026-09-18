package com.atguigu.java.ai.langchain4j.tracking;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyMetricCommand(LocalDate localDate, BigDecimal weightKg, Integer steps,
                                 Integer activityMinutes, Integer sleepMinutes, Integer sleepQuality) { }

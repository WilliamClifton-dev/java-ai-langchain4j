package com.atguigu.java.ai.langchain4j.tracking;

import java.math.BigDecimal;
import java.time.LocalDate;

record WeeklyMetricFact(LocalDate localDate, BigDecimal weightKg, Integer steps,
                        Integer sleepMinutes) { }

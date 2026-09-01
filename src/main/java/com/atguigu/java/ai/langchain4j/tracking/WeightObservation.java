package com.atguigu.java.ai.langchain4j.tracking;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeightObservation(LocalDate localDate, BigDecimal weightKg) { }

package com.atguigu.java.ai.langchain4j.tracking;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record NutritionLog(String id, LocalDate localDate, int energyKcal, BigDecimal proteinG,
                           BigDecimal carbohydrateG, BigDecimal fatG, Instant createdAt) { }

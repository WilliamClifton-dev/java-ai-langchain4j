package com.atguigu.java.ai.langchain4j.tracking;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NutritionCommand(LocalDate localDate, int energyKcal, BigDecimal proteinG,
                               BigDecimal carbohydrateG, BigDecimal fatG) { }

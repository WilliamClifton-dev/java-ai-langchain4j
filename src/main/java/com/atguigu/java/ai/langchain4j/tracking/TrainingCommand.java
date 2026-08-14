package com.atguigu.java.ai.langchain4j.tracking;

import java.time.LocalDate;

public record TrainingCommand(LocalDate localDate, TrainingType trainingType,
                              int durationMinutes, TrainingIntensity intensity) { }

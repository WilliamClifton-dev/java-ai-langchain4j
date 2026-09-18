package com.atguigu.java.ai.langchain4j.tracking;

import java.time.Instant;
import java.time.LocalDate;

public record TrainingLog(String id, LocalDate localDate, TrainingType trainingType,
                          int durationMinutes, TrainingIntensity intensity, Instant createdAt) { }

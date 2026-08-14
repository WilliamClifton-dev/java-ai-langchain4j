package com.atguigu.java.ai.langchain4j.tracking;

import java.time.Instant;
import java.time.LocalDate;

record TrainingRow(String id, LocalDate localDate, TrainingType trainingType, int durationMinutes,
                   TrainingIntensity intensity, Instant createdAt, String payloadHash) {
    TrainingLog domain() { return new TrainingLog(id, localDate, trainingType,
            durationMinutes, intensity, createdAt); }
}

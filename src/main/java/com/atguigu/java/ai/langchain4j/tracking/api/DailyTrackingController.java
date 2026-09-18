package com.atguigu.java.ai.langchain4j.tracking.api;

import com.atguigu.java.ai.langchain4j.tracking.DailyMetric;
import com.atguigu.java.ai.langchain4j.tracking.DailyTrackingService;
import com.atguigu.java.ai.langchain4j.tracking.NutritionLog;
import com.atguigu.java.ai.langchain4j.tracking.TrackingWrite;
import com.atguigu.java.ai.langchain4j.tracking.TrainingLog;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/tracking")
public class DailyTrackingController {
    private final DailyTrackingService service;

    public DailyTrackingController(DailyTrackingService service) {
        this.service = service;
    }

    @PostMapping("/daily-metrics")
    public ResponseEntity<TrackingWriteResponse<DailyMetric>> recordMetric(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody DailyMetricRequest request
    ) {
        return response(service.recordMetric(jwt.getSubject(), idempotencyKey, request.toCommand()));
    }

    @PostMapping("/nutrition")
    public ResponseEntity<TrackingWriteResponse<NutritionLog>> recordNutrition(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody NutritionRequest request
    ) {
        return response(service.recordNutrition(jwt.getSubject(), idempotencyKey, request.toCommand()));
    }

    @PostMapping("/training")
    public ResponseEntity<TrackingWriteResponse<TrainingLog>> recordTraining(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TrainingRequest request
    ) {
        return response(service.recordTraining(jwt.getSubject(), idempotencyKey, request.toCommand()));
    }

    @GetMapping("/days/{date}")
    public DailySummaryResponse summary(@AuthenticationPrincipal Jwt jwt, @PathVariable LocalDate date) {
        return DailySummaryResponse.from(service.summary(jwt.getSubject(), date));
    }

    private <T> ResponseEntity<TrackingWriteResponse<T>> response(TrackingWrite<T> write) {
        HttpStatus status = write.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(TrackingWriteResponse.from(write));
    }
}

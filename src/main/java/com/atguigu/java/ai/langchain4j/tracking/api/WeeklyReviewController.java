package com.atguigu.java.ai.langchain4j.tracking.api;

import com.atguigu.java.ai.langchain4j.tracking.WeeklyReviewNotFoundException;
import com.atguigu.java.ai.langchain4j.tracking.WeeklyReviewService;
import com.atguigu.java.ai.langchain4j.tracking.WeeklyReviewWrite;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tracking/weekly-reviews")
public class WeeklyReviewController {
    private final WeeklyReviewService service;

    public WeeklyReviewController(WeeklyReviewService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<WeeklyReviewWriteResponse> generate(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody WeeklyReviewRequest request
    ) {
        WeeklyReviewWrite write = service.generate(jwt.getSubject(), request.windowEnd());
        HttpStatus status = write.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(WeeklyReviewWriteResponse.from(write));
    }

    @GetMapping("/{reviewId}")
    public WeeklyReviewResponse get(@AuthenticationPrincipal Jwt jwt,
                                    @PathVariable String reviewId) {
        return service.get(jwt.getSubject(), reviewId).map(WeeklyReviewResponse::from)
                .orElseThrow(WeeklyReviewNotFoundException::new);
    }
}

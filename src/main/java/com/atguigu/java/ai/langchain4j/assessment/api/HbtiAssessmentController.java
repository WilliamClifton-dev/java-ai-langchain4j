package com.atguigu.java.ai.langchain4j.assessment.api;

import com.atguigu.java.ai.langchain4j.assessment.AssessmentResultNotFoundException;
import com.atguigu.java.ai.langchain4j.assessment.HbtiAnswer;
import com.atguigu.java.ai.langchain4j.assessment.HbtiAssessmentService;
import com.atguigu.java.ai.langchain4j.assessment.HbtiAssessmentSubmission;
import com.atguigu.java.ai.langchain4j.assessment.HbtiDefinitionCatalog;
import com.atguigu.java.ai.langchain4j.assessment.SubmitHbtiAssessmentCommand;
import com.atguigu.java.ai.langchain4j.infrastructure.redis.RequestLeaseCoordinator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/assessments/hbti")
public class HbtiAssessmentController {

    private final HbtiAssessmentService service;
    private final HbtiDefinitionCatalog definitions;
    private final RequestLeaseCoordinator leaseCoordinator;

    public HbtiAssessmentController(
            HbtiAssessmentService service,
            HbtiDefinitionCatalog definitions,
            RequestLeaseCoordinator leaseCoordinator
    ) {
        this.service = service;
        this.definitions = definitions;
        this.leaseCoordinator = leaseCoordinator;
    }

    @GetMapping("/definitions/{version}")
    public HbtiDefinitionResponse definition(@PathVariable String version) {
        return definitions.findPublished("hbti", version)
                .map(HbtiDefinitionResponse::from)
                .orElseThrow(com.atguigu.java.ai.langchain4j.assessment.AssessmentDefinitionNotFoundException::new);
    }

    @PostMapping("/submissions")
    public ResponseEntity<HbtiAssessmentSubmissionResponse> submit(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody HbtiAssessmentSubmissionRequest request
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return submit(jwt.getSubject(), idempotencyKey, request);
        }
        try (RequestLeaseCoordinator.Lease ignored = leaseCoordinator.acquire(
                "assessment", jwt.getSubject(), idempotencyKey, Duration.ofSeconds(30))) {
            return submit(jwt.getSubject(), idempotencyKey, request);
        }
    }

    private ResponseEntity<HbtiAssessmentSubmissionResponse> submit(
            String userId,
            String idempotencyKey,
            HbtiAssessmentSubmissionRequest request
    ) {
        HbtiAssessmentSubmission submission = service.submit(
                userId, idempotencyKey,
                new SubmitHbtiAssessmentCommand(
                        request.definitionVersion(),
                        request.answers().stream()
                                .map(answer -> new HbtiAnswer(answer.itemKey(), answer.value()))
                                .toList()
                )
        );
        HttpStatus status = submission.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(HbtiAssessmentSubmissionResponse.from(submission));
    }

    @GetMapping("/results/current")
    public HbtiAssessmentResultResponse current(@AuthenticationPrincipal Jwt jwt) {
        return service.current(jwt.getSubject())
                .map(HbtiAssessmentResultResponse::from)
                .orElseThrow(AssessmentResultNotFoundException::new);
    }

    @GetMapping("/results")
    public HbtiAssessmentPageResponse history(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return HbtiAssessmentPageResponse.from(service.history(jwt.getSubject(), page, pageSize));
    }
}

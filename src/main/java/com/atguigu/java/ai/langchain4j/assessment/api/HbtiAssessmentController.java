package com.atguigu.java.ai.langchain4j.assessment.api;

import com.atguigu.java.ai.langchain4j.assessment.AssessmentResultNotFoundException;
import com.atguigu.java.ai.langchain4j.assessment.HbtiAnswer;
import com.atguigu.java.ai.langchain4j.assessment.HbtiAssessmentService;
import com.atguigu.java.ai.langchain4j.assessment.HbtiAssessmentSubmission;
import com.atguigu.java.ai.langchain4j.assessment.SubmitHbtiAssessmentCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assessments/hbti")
public class HbtiAssessmentController {

    private final HbtiAssessmentService service;

    public HbtiAssessmentController(HbtiAssessmentService service) {
        this.service = service;
    }

    @PostMapping("/submissions")
    public ResponseEntity<HbtiAssessmentSubmissionResponse> submit(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody HbtiAssessmentSubmissionRequest request
    ) {
        HbtiAssessmentSubmission submission = service.submit(
                jwt.getSubject(), idempotencyKey,
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

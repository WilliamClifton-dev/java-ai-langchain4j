package com.atguigu.java.ai.langchain4j.planning.api;

import com.atguigu.java.ai.langchain4j.planning.PlanVersionNotFoundException;
import com.atguigu.java.ai.langchain4j.planning.WeightPlanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plans")
public class WeightPlanController {

    private final WeightPlanService service;

    public WeightPlanController(WeightPlanService service) {
        this.service = service;
    }

    @PostMapping("/drafts")
    @ResponseStatus(HttpStatus.CREATED)
    public WeightPlanVersionResponse createDraft(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody WeightPlanDraftRequest request
    ) {
        return WeightPlanVersionResponse.from(
                service.createDraft(jwt.getSubject(), idempotencyKey, request.goal())
        );
    }

    @GetMapping("/{planId}/versions/{versionId}")
    public WeightPlanVersionResponse get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String planId,
            @PathVariable String versionId
    ) {
        return WeightPlanVersionResponse.from(service.get(jwt.getSubject(), planId, versionId));
    }

    @GetMapping("/active")
    public WeightPlanVersionResponse active(@AuthenticationPrincipal Jwt jwt) {
        return service.currentActive(jwt.getSubject())
                .map(WeightPlanVersionResponse::from)
                .orElseThrow(PlanVersionNotFoundException::new);
    }

    @PostMapping("/{planId}/versions/{versionId}/validation")
    public WeightPlanVersionResponse validate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String planId,
            @PathVariable String versionId
    ) {
        return WeightPlanVersionResponse.from(
                service.validate(jwt.getSubject(), planId, versionId)
        );
    }

    @PostMapping("/{planId}/versions/{versionId}/confirmation")
    public WeightPlanVersionResponse confirm(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String planId,
            @PathVariable String versionId
    ) {
        return WeightPlanVersionResponse.from(
                service.confirm(jwt.getSubject(), planId, versionId)
        );
    }

    @PostMapping("/{planId}/versions/{versionId}/activation")
    public WeightPlanVersionResponse activate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String planId,
            @PathVariable String versionId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return WeightPlanVersionResponse.from(
                service.activate(jwt.getSubject(), planId, versionId, idempotencyKey)
        );
    }
}

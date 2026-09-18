package com.atguigu.java.ai.langchain4j.identity.api;

import com.atguigu.java.ai.langchain4j.common.audit.AuditEvent;
import com.atguigu.java.ai.langchain4j.common.audit.AuditEventService;
import com.atguigu.java.ai.langchain4j.common.audit.AuditEventType;
import com.atguigu.java.ai.langchain4j.identity.AccountDataExportService;
import com.atguigu.java.ai.langchain4j.identity.AccountDeletionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

    private final AccountDataExportService exportService;
    private final AccountDeletionService deletionService;
    private final AuthCookieWriter authCookieWriter;
    private final AuditEventService auditEvents;

    public AccountController(AccountDataExportService exportService,
                              AccountDeletionService deletionService,
                              AuthCookieWriter authCookieWriter,
                              AuditEventService auditEvents) {
        this.exportService = exportService;
        this.deletionService = deletionService;
        this.authCookieWriter = authCookieWriter;
        this.auditEvents = auditEvents;
    }

    @GetMapping("/data-export")
    public AccountDataExport export(@AuthenticationPrincipal Jwt jwt,
                                    HttpServletRequest request) {
        AccountDataExport export = exportService.export(jwt.getSubject());
        audit(AuditEventType.DATA_EXPORT_REQUESTED, jwt.getSubject(), request, true,
                Map.of("action", "data_export", "outcome", "completed"));
        return export;
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DeleteAccountRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        audit(AuditEventType.DATA_DELETION_REQUESTED, jwt.getSubject(), httpRequest, true,
                Map.of("action", "account_deletion", "outcome", "requested"));
        deletionService.delete(jwt.getSubject(), request.confirmation());
        authCookieWriter.clearSession(response);
        audit(AuditEventType.ACCOUNT_DELETED, null, httpRequest, true,
                Map.of("action", "account_deletion", "outcome", "completed"));
        return ResponseEntity.noContent().build();
    }

    private void audit(AuditEventType type, String userId, HttpServletRequest request,
                       boolean success, Map<String, Object> details) {
        auditEvents.record(AuditEvent.create(type, userId, request.getRemoteAddr(), success,
                details));
    }

    public record DeleteAccountRequest(@NotBlank String confirmation) { }
}

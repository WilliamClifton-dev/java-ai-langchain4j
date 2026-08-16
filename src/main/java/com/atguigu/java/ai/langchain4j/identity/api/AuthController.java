package com.atguigu.java.ai.langchain4j.identity.api;

import com.atguigu.java.ai.langchain4j.identity.AuthSession;
import com.atguigu.java.ai.langchain4j.identity.AuthenticationService;
import com.atguigu.java.ai.langchain4j.identity.InvalidCredentialsException;
import com.atguigu.java.ai.langchain4j.identity.LoginAttemptGuard;
import com.atguigu.java.ai.langchain4j.identity.RegisterAccountCommand;
import com.atguigu.java.ai.langchain4j.identity.RefreshTokenReuseException;
import com.atguigu.java.ai.langchain4j.identity.RegisteredAccount;
import com.atguigu.java.ai.langchain4j.identity.TooManyLoginAttemptsException;
import com.atguigu.java.ai.langchain4j.common.audit.AuditEvent;
import com.atguigu.java.ai.langchain4j.common.audit.AuditEventService;
import com.atguigu.java.ai.langchain4j.common.audit.AuditEventType;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final AuthCookieWriter authCookieWriter;
    private final LoginAttemptGuard loginAttemptGuard;
    private final AuditEventService auditEvents;

    public AuthController(
            AuthenticationService authenticationService,
            AuthCookieWriter authCookieWriter,
            LoginAttemptGuard loginAttemptGuard,
            AuditEventService auditEvents
    ) {
        this.authenticationService = authenticationService;
        this.authCookieWriter = authCookieWriter;
        this.loginAttemptGuard = loginAttemptGuard;
        this.auditEvents = auditEvents;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @PostMapping("/register")
    public ResponseEntity<AuthSessionResponse> register(
            @Valid @RequestBody AuthCredentialsRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        AuthSession session = authenticationService.register(
                new RegisterAccountCommand(request.email(), request.password())
        );
        authCookieWriter.writeSession(response, session);
        audit(AuditEventType.ACCOUNT_REGISTERED, session.account().id(), httpRequest,
                true, Map.of("action", "register"));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(session));
    }

    @GetMapping("/session")
    public AuthSessionResponse session(@AuthenticationPrincipal Jwt jwt) {
        RegisteredAccount account = authenticationService.currentAccount(jwt.getSubject());
        return new AuthSessionResponse(
                new AuthSessionResponse.User(account.id(), account.email()),
                jwt.getExpiresAt()
        );
    }

    @PostMapping("/login")
    public AuthSessionResponse login(
            @Valid @RequestBody AuthCredentialsRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        String attemptKey = LoginAttemptGuard.key(httpRequest.getRemoteAddr(), request.email());
        try {
            loginAttemptGuard.assertAllowed(attemptKey);
            AuthSession session = authenticationService.login(request.email(), request.password());
            loginAttemptGuard.recordSuccess(attemptKey);
            authCookieWriter.writeSession(response, session);
            audit(AuditEventType.LOGIN_SUCCESS, session.account().id(), httpRequest,
                    true, Map.of("action", "login"));
            return toResponse(session);
        } catch (InvalidCredentialsException | TooManyLoginAttemptsException exception) {
            if (exception instanceof InvalidCredentialsException) {
                loginAttemptGuard.recordFailure(attemptKey);
            }
            String reason = exception instanceof TooManyLoginAttemptsException
                    ? "RATE_LIMITED" : "INVALID_CREDENTIALS";
            audit(AuditEventType.LOGIN_FAILURE, null, httpRequest,
                    false, Map.of("action", "login", "reasonCode", reason));
            throw exception;
        }
    }

    @PostMapping("/refresh")
    public AuthSessionResponse refresh(
            @CookieValue(AuthCookieWriter.REFRESH_COOKIE) String refreshToken,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        try {
            AuthSession session = authenticationService.refresh(refreshToken);
            authCookieWriter.writeSession(response, session);
            audit(AuditEventType.TOKEN_REFRESH, session.account().id(), httpRequest,
                    true, Map.of("action", "refresh"));
            return toResponse(session);
        } catch (RefreshTokenReuseException exception) {
            audit(AuditEventType.TOKEN_REUSE_DETECTED, null, httpRequest,
                    false, Map.of("action", "refresh", "reasonCode", "TOKEN_REUSE"));
            throw exception;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = AuthCookieWriter.REFRESH_COOKIE, required = false) String refreshToken,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        if (refreshToken != null) {
            authenticationService.logout(refreshToken);
        }
        authCookieWriter.clearSession(response);
        audit(AuditEventType.LOGOUT, jwt == null ? null : jwt.getSubject(), httpRequest,
                true, Map.of("action", "logout"));
        return ResponseEntity.noContent().build();
    }

    private void audit(AuditEventType type, String userId, HttpServletRequest request,
                       boolean success, Map<String, Object> details) {
        auditEvents.record(AuditEvent.create(
                type, userId, request.getRemoteAddr(), success, details));
    }

    private AuthSessionResponse toResponse(AuthSession session) {
        return new AuthSessionResponse(
                new AuthSessionResponse.User(
                        session.account().id(),
                        session.account().email()
                ),
                session.accessToken().expiresAt()
        );
    }
}

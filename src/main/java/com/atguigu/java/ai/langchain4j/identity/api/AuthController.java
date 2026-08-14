package com.atguigu.java.ai.langchain4j.identity.api;

import com.atguigu.java.ai.langchain4j.identity.AuthSession;
import com.atguigu.java.ai.langchain4j.identity.AuthenticationService;
import com.atguigu.java.ai.langchain4j.identity.InvalidCredentialsException;
import com.atguigu.java.ai.langchain4j.identity.LoginAttemptGuard;
import com.atguigu.java.ai.langchain4j.identity.RegisterAccountCommand;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
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

    public AuthController(
            AuthenticationService authenticationService,
            AuthCookieWriter authCookieWriter,
            LoginAttemptGuard loginAttemptGuard
    ) {
        this.authenticationService = authenticationService;
        this.authCookieWriter = authCookieWriter;
        this.loginAttemptGuard = loginAttemptGuard;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @PostMapping("/register")
    public ResponseEntity<AuthSessionResponse> register(
            @Valid @RequestBody AuthCredentialsRequest request,
            HttpServletResponse response
    ) {
        AuthSession session = authenticationService.register(
                new RegisterAccountCommand(request.email(), request.password())
        );
        authCookieWriter.writeSession(response, session);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(session));
    }

    @PostMapping("/login")
    public AuthSessionResponse login(
            @Valid @RequestBody AuthCredentialsRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        String attemptKey = LoginAttemptGuard.key(httpRequest.getRemoteAddr(), request.email());
        loginAttemptGuard.assertAllowed(attemptKey);
        try {
            AuthSession session = authenticationService.login(request.email(), request.password());
            loginAttemptGuard.recordSuccess(attemptKey);
            authCookieWriter.writeSession(response, session);
            return toResponse(session);
        } catch (InvalidCredentialsException exception) {
            loginAttemptGuard.recordFailure(attemptKey);
            throw exception;
        }
    }

    @PostMapping("/refresh")
    public AuthSessionResponse refresh(
            @CookieValue(AuthCookieWriter.REFRESH_COOKIE) String refreshToken,
            HttpServletResponse response
    ) {
        AuthSession session = authenticationService.refresh(refreshToken);
        authCookieWriter.writeSession(response, session);
        return toResponse(session);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = AuthCookieWriter.REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken != null) {
            authenticationService.logout(refreshToken);
        }
        authCookieWriter.clearSession(response);
        return ResponseEntity.noContent().build();
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

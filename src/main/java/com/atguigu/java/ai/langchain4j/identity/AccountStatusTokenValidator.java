package com.atguigu.java.ai.langchain4j.identity;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

/** Rejects otherwise-valid access tokens after an account has been disabled or deleted. */
public final class AccountStatusTokenValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error INACTIVE_ACCOUNT = new OAuth2Error(
            "invalid_token", "Account is not active", null);

    private final UserAccountMapper accounts;

    public AccountStatusTokenValidator(UserAccountMapper accounts) {
        this.accounts = accounts;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        try {
            Optional<UserAccount> account = accounts.findById(token.getSubject());
            return account.filter(candidate -> candidate.status() == AccountStatus.ACTIVE)
                    .map(candidate -> OAuth2TokenValidatorResult.success())
                    .orElseGet(() -> OAuth2TokenValidatorResult.failure(INACTIVE_ACCOUNT));
        } catch (RuntimeException ignored) {
            return OAuth2TokenValidatorResult.failure(INACTIVE_ACCOUNT);
        }
    }
}

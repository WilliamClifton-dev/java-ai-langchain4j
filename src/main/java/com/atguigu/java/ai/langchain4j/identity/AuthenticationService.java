package com.atguigu.java.ai.langchain4j.identity;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private final AccountRegistrationService registrationService;
    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokenService;
    private final RefreshTokenService refreshTokenService;
    private final String dummyPasswordHash;

    public AuthenticationService(
            AccountRegistrationService registrationService,
            UserAccountMapper userAccountMapper,
            PasswordEncoder passwordEncoder,
            AccessTokenService accessTokenService,
            RefreshTokenService refreshTokenService
    ) {
        this.registrationService = registrationService;
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenService = accessTokenService;
        this.refreshTokenService = refreshTokenService;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-not-used");
    }

    @Transactional
    public AuthSession register(RegisterAccountCommand command) {
        RegisteredAccount account = registrationService.register(command);
        return issueSession(account);
    }

    @Transactional
    public AuthSession login(String email, String password) {
        try {
            AccountRegistrationService.validatePassword(password);
        } catch (InvalidCredentialInputException exception) {
            throw new InvalidCredentialsException();
        }
        String normalizedEmail;
        try {
            normalizedEmail = AccountRegistrationService.normalizeEmail(email);
        } catch (IllegalArgumentException exception) {
            verifyDummy(password);
            throw new InvalidCredentialsException();
        }

        UserAccount account = userAccountMapper.findByNormalizedEmail(normalizedEmail).orElse(null);
        String candidateHash = account == null ? dummyPasswordHash : account.passwordHash();
        boolean passwordMatches = password != null && passwordEncoder.matches(password, candidateHash);
        if (!passwordMatches || account == null || account.status() != AccountStatus.ACTIVE) {
            throw new InvalidCredentialsException();
        }
        return issueSession(toRegisteredAccount(account));
    }

    @Transactional(noRollbackFor = RefreshTokenReuseException.class)
    public AuthSession refresh(String rawRefreshToken) {
        IssuedRefreshToken refreshToken = refreshTokenService.rotate(rawRefreshToken);
        UserAccount account = userAccountMapper.findById(refreshToken.userId())
                .filter(candidate -> candidate.status() == AccountStatus.ACTIVE)
                .orElseThrow(InvalidRefreshTokenException::new);
        return new AuthSession(
                toRegisteredAccount(account),
                accessTokenService.issue(account.id()),
                refreshToken
        );
    }

    public void logout(String rawRefreshToken) {
        try {
            refreshTokenService.revokeFamily(rawRefreshToken);
        } catch (InvalidRefreshTokenException ignored) {
            // Logout remains idempotent and never reveals token validity.
        }
    }

    @Transactional(readOnly = true)
    public RegisteredAccount currentAccount(String userId) {
        UserAccount account = userAccountMapper.findById(userId)
                .filter(candidate -> candidate.status() == AccountStatus.ACTIVE)
                .orElseThrow(InvalidCredentialsException::new);
        return toRegisteredAccount(account);
    }

    private AuthSession issueSession(RegisteredAccount account) {
        return new AuthSession(
                account,
                accessTokenService.issue(account.id()),
                refreshTokenService.issue(account.id())
        );
    }

    private RegisteredAccount toRegisteredAccount(UserAccount account) {
        return new RegisteredAccount(
                account.id(), account.normalizedEmail(), account.status(), account.createdAt()
        );
    }

    private void verifyDummy(String password) {
        if (password != null) {
            passwordEncoder.matches(password, dummyPasswordHash);
        }
    }
}

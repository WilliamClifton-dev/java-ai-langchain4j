package com.atguigu.java.ai.langchain4j.identity;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class AccountRegistrationService {

    static final int MINIMUM_PASSWORD_CHARACTERS = 12;
    static final int MAXIMUM_BCRYPT_BYTES = 72;

    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AccountRegistrationService(
            UserAccountMapper userAccountMapper,
            PasswordEncoder passwordEncoder,
            Clock clock
    ) {
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public RegisteredAccount register(RegisterAccountCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String normalizedEmail = normalizeEmail(command.email());
        validatePassword(command.password());

        Instant now = clock.instant();
        UserAccount account = new UserAccount(
                UUID.randomUUID().toString(),
                normalizedEmail,
                passwordEncoder.encode(command.password()),
                AccountStatus.ACTIVE,
                now,
                now
        );
        try {
            userAccountMapper.insert(account);
        } catch (DuplicateKeyException exception) {
            throw new EmailAlreadyRegisteredException();
        }
        return new RegisteredAccount(account.id(), normalizedEmail, account.status(), now);
    }

    static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidCredentialInputException("email must not be blank");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 254 || !normalized.contains("@")) {
            throw new InvalidCredentialInputException("email is invalid");
        }
        return normalized;
    }

    static void validatePassword(String password) {
        if (password == null || password.length() < MINIMUM_PASSWORD_CHARACTERS) {
            throw new InvalidCredentialInputException("password must contain at least 12 characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAXIMUM_BCRYPT_BYTES) {
            throw new InvalidCredentialInputException("password must not exceed 72 UTF-8 bytes");
        }
    }
}

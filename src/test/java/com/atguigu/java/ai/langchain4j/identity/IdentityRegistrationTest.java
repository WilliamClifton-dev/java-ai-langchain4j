package com.atguigu.java.ai.langchain4j.identity;

import com.atguigu.java.ai.langchain4j.config.TimeConfig;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@MybatisTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
@Import({AccountRegistrationService.class, IdentityCredentialConfig.class, TimeConfig.class})
class IdentityRegistrationTest {

    @Autowired
    private AccountRegistrationService registrationService;

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void normalizesTheEmailAndStoresOnlyAnAdaptivePasswordHash() {
        RegisteredAccount registered = registrationService.register(
                new RegisterAccountCommand("  User@Example.COM ", "correct horse battery staple")
        );

        UserAccount stored = userAccountMapper.findById(registered.id()).orElseThrow();
        assertThat(registered.email()).isEqualTo("user@example.com");
        assertThat(stored.normalizedEmail()).isEqualTo("user@example.com");
        assertThat(stored.passwordHash()).isNotEqualTo("correct horse battery staple");
        assertThat(passwordEncoder.matches("correct horse battery staple", stored.passwordHash())).isTrue();
        assertThat(stored.status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void rejectsDuplicateNormalizedEmails() {
        registrationService.register(new RegisterAccountCommand(
                "user@example.com",
                "correct horse battery staple"
        ));

        assertThatThrownBy(() -> registrationService.register(new RegisterAccountCommand(
                " USER@example.com ",
                "another correct password"
        ))).isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void rejectsWeakOrOverlongPasswordsBeforeHashing() {
        String overlongUtf8Password = "测".repeat(25);
        assertThat(overlongUtf8Password.getBytes(StandardCharsets.UTF_8)).hasSizeGreaterThan(72);

        assertThatThrownBy(() -> registrationService.register(new RegisterAccountCommand(
                "weak@example.com",
                "short"
        ))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registrationService.register(new RegisterAccountCommand(
                "long@example.com",
                overlongUtf8Password
        ))).isInstanceOf(IllegalArgumentException.class);
    }
}

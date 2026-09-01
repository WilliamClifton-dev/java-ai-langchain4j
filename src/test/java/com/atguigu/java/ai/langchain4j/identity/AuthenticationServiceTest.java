package com.atguigu.java.ai.langchain4j.identity;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class AuthenticationServiceTest {

    @Test
    void rejectsLoginPasswordsBeyondBcryptsByteLimitBeforeComparison() {
        PasswordEncoder encoder = new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return "dummy-hash";
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                throw new AssertionError("overlong password reached the encoder");
            }
        };
        AuthenticationService service = new AuthenticationService(
                mock(AccountRegistrationService.class),
                mock(UserAccountMapper.class),
                encoder,
                mock(AccessTokenService.class),
                mock(RefreshTokenService.class)
        );

        assertThatThrownBy(() -> service.login(
                "user@example.com",
                "a".repeat(73)
        )).isInstanceOf(InvalidCredentialsException.class);
    }
}

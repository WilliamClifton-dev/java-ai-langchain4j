package com.atguigu.java.ai.langchain4j.identity;

import com.atguigu.java.ai.langchain4j.config.TimeConfig;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@MybatisTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
@Import({RefreshTokenService.class, AccountRegistrationService.class,
        IdentityCredentialConfig.class, AuthTokenConfig.class, TimeConfig.class})
class RefreshTokenRotationTest {

    @Autowired
    private AccountRegistrationService registrationService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenMapper refreshTokenMapper;

    @Test
    void rotatesAnOpaqueTokenAndStoresOnlySha256Digests() {
        RegisteredAccount account = registrationService.register(new RegisterAccountCommand(
                "token-rotation@example.com", "correct horse battery staple"
        ));

        IssuedRefreshToken first = refreshTokenService.issue(account.id());
        IssuedRefreshToken second = refreshTokenService.rotate(first.value());

        RefreshToken storedFirst = refreshTokenMapper.findById(first.id()).orElseThrow();
        RefreshToken storedSecond = refreshTokenMapper.findById(second.id()).orElseThrow();
        assertThat(storedFirst.tokenHash()).hasSize(64).isNotEqualTo(first.value());
        assertThat(storedFirst.replacedByTokenId()).isEqualTo(second.id());
        assertThat(storedFirst.revokedAt()).isNotNull();
        assertThat(storedSecond.familyId()).isEqualTo(storedFirst.familyId());
    }

    @Test
    void revokesTheTokenFamilyWhenAReplacedTokenIsReused() {
        RegisteredAccount account = registrationService.register(new RegisterAccountCommand(
                "replay@example.com", "correct horse battery staple"
        ));
        IssuedRefreshToken first = refreshTokenService.issue(account.id());
        IssuedRefreshToken second = refreshTokenService.rotate(first.value());

        assertThatThrownBy(() -> refreshTokenService.rotate(first.value()))
                .isInstanceOf(RefreshTokenReuseException.class);

        assertThat(refreshTokenMapper.findById(second.id()).orElseThrow().revokedAt()).isNotNull();
    }
}

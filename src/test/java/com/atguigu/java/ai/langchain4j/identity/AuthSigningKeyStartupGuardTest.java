package com.atguigu.java.ai.langchain4j.identity;

import com.atguigu.java.ai.langchain4j.HbtiCoachApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end smoke: stand up {@link SpringApplication} with the development
 * signing key under the {@code minimax} profile and confirm the
 * {@link AuthSigningKeyEnvironmentPostProcessor} aborts the boot before the
 * application context refreshes.
 *
 * <p>The companion case ("custom key boots normally") is already covered by
 * the broader {@code @SpringBootTest} suite, so this class deliberately only
 * exercises the rejection path.
 */
class AuthSigningKeyStartupGuardTest {

    @Test
    void refusesToBootUnderMinimaxProfileWhenDevKeyIsConfigured() {
        SpringApplication application = new SpringApplication(HbtiCoachApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setAdditionalProfiles("minimax");
        application.setLogStartupInfo(false);

        assertThatThrownBy(() -> application.run(
                "--hbti.auth.signing-key=" + AuthSigningKeyDefaults.DEFAULT_DEVELOPMENT_KEY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("well-known development value")
                .hasMessageContaining("minimax");
    }
}

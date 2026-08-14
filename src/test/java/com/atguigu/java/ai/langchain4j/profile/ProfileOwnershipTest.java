package com.atguigu.java.ai.langchain4j.profile;

import com.atguigu.java.ai.langchain4j.config.TimeConfig;
import com.atguigu.java.ai.langchain4j.identity.AccountRegistrationService;
import com.atguigu.java.ai.langchain4j.identity.IdentityCredentialConfig;
import com.atguigu.java.ai.langchain4j.identity.RegisterAccountCommand;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@MybatisTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
@Import({ProfileService.class, SafetyScreeningPolicy.class,
        AccountRegistrationService.class, IdentityCredentialConfig.class, TimeConfig.class})
class ProfileOwnershipTest {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private AccountRegistrationService registrationService;

    @Test
    void profileAndScreeningQueriesAreScopedByTheAuthenticatedOwner() {
        String ownerId = registrationService.register(new RegisterAccountCommand(
                "profile-owner@example.com", "correct horse battery staple"
        )).id();
        String otherId = registrationService.register(new RegisterAccountCommand(
                "profile-other@example.com", "correct horse battery staple"
        )).id();

        profileService.save(ownerId, command());
        SafetyScreening screening = profileService.screen(ownerId, new SafetyScreeningAnswers(
                false, false, false, false, false
        ));

        assertThat(profileService.find(ownerId)).isPresent();
        assertThat(profileService.find(otherId)).isEmpty();
        assertThat(profileService.findCurrentScreening(ownerId)).contains(screening);
        assertThat(profileService.findCurrentScreening(otherId)).isEmpty();
    }

    private SaveProfileCommand command() {
        return new SaveProfileCommand(
                LocalDate.of(1992, 4, 20), CalculationSex.MALE,
                178, 82, 74, ActivityLevel.LIGHT, "Asia/Hong_Kong"
        );
    }
}

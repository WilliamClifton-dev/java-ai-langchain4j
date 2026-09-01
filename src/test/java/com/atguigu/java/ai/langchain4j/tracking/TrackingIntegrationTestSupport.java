package com.atguigu.java.ai.langchain4j.tracking;

import com.atguigu.java.ai.langchain4j.config.TimeConfig;
import com.atguigu.java.ai.langchain4j.identity.AccountRegistrationService;
import com.atguigu.java.ai.langchain4j.identity.IdentityCredentialConfig;
import com.atguigu.java.ai.langchain4j.identity.RegisterAccountCommand;
import com.atguigu.java.ai.langchain4j.profile.ActivityLevel;
import com.atguigu.java.ai.langchain4j.profile.CalculationSex;
import com.atguigu.java.ai.langchain4j.profile.ProfileService;
import com.atguigu.java.ai.langchain4j.profile.SafetyScreeningPolicy;
import com.atguigu.java.ai.langchain4j.profile.SaveProfileCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
@Import({DailyTrackingService.class, ProfileService.class, SafetyScreeningPolicy.class,
        AccountRegistrationService.class, IdentityCredentialConfig.class, TimeConfig.class})
abstract class TrackingIntegrationTestSupport {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    protected DailyTrackingService service;

    @Autowired
    private AccountRegistrationService registration;

    @Autowired
    private ProfileService profiles;

    protected String user(String prefix) {
        String userId = registration.register(new RegisterAccountCommand(
                prefix + "-" + SEQUENCE.incrementAndGet() + "@example.com",
                "correct horse battery staple"
        )).id();
        profiles.save(userId, new SaveProfileCommand(
                LocalDate.of(1990, 1, 1), CalculationSex.FEMALE,
                165, 70, 60, ActivityLevel.MODERATE, "Asia/Hong_Kong"
        ));
        return userId;
    }
}

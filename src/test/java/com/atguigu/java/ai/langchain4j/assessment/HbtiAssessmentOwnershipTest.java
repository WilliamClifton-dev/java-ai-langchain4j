package com.atguigu.java.ai.langchain4j.assessment;

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

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@MybatisTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
@Import({HbtiAssessmentService.class, HbtiDefinitionRepository.class, HbtiScoringEngine.class,
        AccountRegistrationService.class, IdentityCredentialConfig.class, TimeConfig.class})
class HbtiAssessmentOwnershipTest {

    @Autowired
    private HbtiAssessmentService service;

    @Autowired
    private AccountRegistrationService registrationService;

    @Test
    void currentAndHistoryQueriesAreScopedToTheirOwner() {
        String ownerId = user("assessment-owner@example.com");
        String otherId = user("assessment-other@example.com");
        HbtiAssessmentResult result = service.submit(
                ownerId, "owner-request", command()
        ).result();

        assertThat(service.current(ownerId)).contains(result);
        assertThat(service.history(ownerId, 0, 20).items()).containsExactly(result);
        assertThat(service.current(otherId)).isEmpty();
        assertThat(service.history(otherId, 0, 20).items()).isEmpty();
    }

    private String user(String email) {
        return registrationService.register(new RegisterAccountCommand(
                email, "correct horse battery staple"
        )).id();
    }

    private SubmitHbtiAssessmentCommand command() {
        List<HbtiAnswer> answers = IntStream.rangeClosed(1, 16)
                .mapToObj(index -> new HbtiAnswer("q" + index, 3))
                .toList();
        return new SubmitHbtiAssessmentCommand("1.0.0", answers);
    }
}

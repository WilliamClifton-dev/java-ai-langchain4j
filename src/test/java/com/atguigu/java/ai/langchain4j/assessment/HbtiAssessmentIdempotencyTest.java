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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@MybatisTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
@Import({HbtiAssessmentService.class, HbtiDefinitionRepository.class, HbtiScoringEngine.class,
        AccountRegistrationService.class, IdentityCredentialConfig.class, TimeConfig.class})
class HbtiAssessmentIdempotencyTest {

    @Autowired
    private HbtiAssessmentService service;

    @Autowired
    private AccountRegistrationService registrationService;

    @Test
    void sameKeyAndPayloadReturnsTheOriginalCompletedResult() {
        String userId = user("assessment-idempotent@example.com");
        SubmitHbtiAssessmentCommand command = command(3);

        HbtiAssessmentSubmission first = service.submit(userId, "request-1", command);
        HbtiAssessmentSubmission replay = service.submit(userId, "request-1", command);

        assertThat(first.replayed()).isFalse();
        assertThat(replay.replayed()).isTrue();
        assertThat(replay.result()).isEqualTo(first.result());
        assertThat(service.history(userId, 0, 20).items()).hasSize(1);
    }

    @Test
    void sameKeyWithDifferentPayloadIsAConflict() {
        String userId = user("assessment-conflict@example.com");
        service.submit(userId, "request-2", command(3));

        List<HbtiAnswer> changed = new ArrayList<>(command(3).answers());
        changed.set(0, new HbtiAnswer("q1", 4));

        assertThatThrownBy(() -> service.submit(
                userId, "request-2", new SubmitHbtiAssessmentCommand("1.0.0", changed)
        )).isInstanceOf(IdempotencyConflictException.class);
    }

    private String user(String email) {
        return registrationService.register(new RegisterAccountCommand(
                email, "correct horse battery staple"
        )).id();
    }

    private SubmitHbtiAssessmentCommand command(int value) {
        List<HbtiAnswer> answers = new ArrayList<>();
        for (int index = 1; index <= 16; index++) {
            answers.add(new HbtiAnswer("q" + index, value));
        }
        return new SubmitHbtiAssessmentCommand("1.0.0", answers);
    }
}

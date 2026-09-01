package com.atguigu.java.ai.langchain4j.planning;

import com.atguigu.java.ai.langchain4j.assessment.HbtiAssessmentService;
import com.atguigu.java.ai.langchain4j.assessment.HbtiDefinitionRepository;
import com.atguigu.java.ai.langchain4j.assessment.HbtiScoringEngine;
import com.atguigu.java.ai.langchain4j.config.TimeConfig;
import com.atguigu.java.ai.langchain4j.identity.AccountRegistrationService;
import com.atguigu.java.ai.langchain4j.identity.IdentityCredentialConfig;
import com.atguigu.java.ai.langchain4j.profile.ProfileService;
import com.atguigu.java.ai.langchain4j.profile.SafetyScreeningPolicy;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@MybatisTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
@Import({WeightPlanService.class, HealthCalculator.class, TargetRangePolicy.class,
        PlanningEligibilityPolicy.class, ProfileService.class, SafetyScreeningPolicy.class,
        HbtiAssessmentService.class, HbtiDefinitionRepository.class, HbtiScoringEngine.class,
        AccountRegistrationService.class, IdentityCredentialConfig.class, TimeConfig.class})
class PlanOwnershipTest extends PlanningIntegrationTestSupport {

    @Test
    void anotherUserCannotReadOrTransitionAnOwnedVersion() {
        String ownerId = eligibleUser("plan-owner");
        String otherId = eligibleUser("plan-other");
        WeightPlanVersion draft = service.createDraft(ownerId, "owned-draft", WeightGoal.LOSS);

        assertThatThrownBy(() -> service.get(otherId, draft.planId(), draft.id()))
                .isInstanceOf(PlanVersionNotFoundException.class);
        assertThatThrownBy(() -> service.validate(otherId, draft.planId(), draft.id()))
                .isInstanceOf(PlanVersionNotFoundException.class);
    }
}

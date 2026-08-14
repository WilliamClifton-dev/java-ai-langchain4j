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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@MybatisTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
@Import({WeightPlanService.class, HealthCalculator.class, TargetRangePolicy.class,
        PlanningEligibilityPolicy.class, ProfileService.class, SafetyScreeningPolicy.class,
        HbtiAssessmentService.class, HbtiDefinitionRepository.class, HbtiScoringEngine.class,
        AccountRegistrationService.class, IdentityCredentialConfig.class, TimeConfig.class})
class PlanLifecycleTest extends PlanningIntegrationTestSupport {

    @Test
    void movesThroughRequiredStatesAndAtomicallyReplacesTheActiveVersion() {
        String userId = eligibleUser("plan-lifecycle");
        WeightPlanVersion first = service.createDraft(userId, "first-draft", WeightGoal.LOSS);
        WeightPlanVersion replayedDraft = service.createDraft(
                userId, "first-draft", WeightGoal.LOSS
        );

        assertThat(replayedDraft).isEqualTo(first);
        assertThatThrownBy(() -> service.createDraft(
                userId, "first-draft", WeightGoal.GAIN
        )).isInstanceOf(PlanIdempotencyConflictException.class);
        assertThat(first.status()).isEqualTo(PlanVersionStatus.DRAFT);
        assertThat(first.versionNo()).isEqualTo(1);
        assertThat(first.formulaVersion()).isEqualTo("MIFFLIN_ST_JEOR_METRIC_V1");
        assertThat(first.targetPolicyVersion()).isEqualTo("CONSERVATIVE_ENERGY_RANGE_V1");
        assertThatThrownBy(() -> service.activate(
                userId, first.planId(), first.id(), "premature-activation"
        ))
                .isInstanceOf(InvalidPlanTransitionException.class);

        WeightPlanVersion validated = service.validate(userId, first.planId(), first.id());
        WeightPlanVersion confirmed = service.confirm(userId, first.planId(), first.id());
        WeightPlanVersion active = service.activate(
                userId, first.planId(), first.id(), "first-activation"
        );

        assertThat(validated.status()).isEqualTo(PlanVersionStatus.VALIDATED);
        assertThat(confirmed.status()).isEqualTo(PlanVersionStatus.CONFIRMED);
        assertThat(active.status()).isEqualTo(PlanVersionStatus.ACTIVE);
        assertThat(service.activate(
                userId, first.planId(), first.id(), "first-activation"
        )).isEqualTo(active);
        assertThat(service.currentActive(userId)).contains(active);
        assertThatThrownBy(() -> service.confirm(userId, first.planId(), first.id()))
                .isInstanceOf(InvalidPlanTransitionException.class);

        WeightPlanVersion replacement = confirmedVersion(userId, WeightGoal.MAINTENANCE);
        WeightPlanVersion replacementActive = service.activate(
                userId, replacement.planId(), replacement.id(), "replacement-activation"
        );

        assertThat(replacement.versionNo()).isEqualTo(2);
        assertThat(replacementActive.status()).isEqualTo(PlanVersionStatus.ACTIVE);
        assertThat(service.get(userId, first.planId(), first.id()).status())
                .isEqualTo(PlanVersionStatus.REPLACED);
        assertThat(service.currentActive(userId)).contains(replacementActive);
    }

    @Test
    void rejectsConfirmationAfterItsProfileAndScreeningProvenanceBecomesStale() {
        String userId = eligibleUser("plan-stale");
        WeightPlanVersion draft = service.createDraft(userId, "stale-draft", WeightGoal.LOSS);
        service.validate(userId, draft.planId(), draft.id());

        changeProfileWeight(userId, 71);

        assertThatThrownBy(() -> service.confirm(userId, draft.planId(), draft.id()))
                .isInstanceOf(PlanningPrerequisiteException.class);
    }
}

package com.atguigu.java.ai.langchain4j.tracking;

import com.atguigu.java.ai.langchain4j.assessment.HbtiAnswer;
import com.atguigu.java.ai.langchain4j.assessment.HbtiAssessmentService;
import com.atguigu.java.ai.langchain4j.assessment.HbtiDefinitionRepository;
import com.atguigu.java.ai.langchain4j.assessment.HbtiScoringEngine;
import com.atguigu.java.ai.langchain4j.assessment.SubmitHbtiAssessmentCommand;
import com.atguigu.java.ai.langchain4j.config.TimeConfig;
import com.atguigu.java.ai.langchain4j.identity.AccountRegistrationService;
import com.atguigu.java.ai.langchain4j.identity.IdentityCredentialConfig;
import com.atguigu.java.ai.langchain4j.identity.RegisterAccountCommand;
import com.atguigu.java.ai.langchain4j.planning.HealthCalculator;
import com.atguigu.java.ai.langchain4j.planning.PlanningEligibilityPolicy;
import com.atguigu.java.ai.langchain4j.planning.TargetRangePolicy;
import com.atguigu.java.ai.langchain4j.planning.WeightGoal;
import com.atguigu.java.ai.langchain4j.planning.WeightPlanService;
import com.atguigu.java.ai.langchain4j.planning.WeightPlanVersion;
import com.atguigu.java.ai.langchain4j.profile.ActivityLevel;
import com.atguigu.java.ai.langchain4j.profile.CalculationSex;
import com.atguigu.java.ai.langchain4j.profile.ProfileService;
import com.atguigu.java.ai.langchain4j.profile.SafetyScreeningAnswers;
import com.atguigu.java.ai.langchain4j.profile.SafetyScreeningPolicy;
import com.atguigu.java.ai.langchain4j.profile.SaveProfileCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@ActiveProfiles("test")
@Import({WeeklyReviewService.class, WeeklyReviewPolicy.class, DailyTrackingService.class,
        WeightPlanService.class, HealthCalculator.class, TargetRangePolicy.class,
        PlanningEligibilityPolicy.class, ProfileService.class, SafetyScreeningPolicy.class,
        HbtiAssessmentService.class, HbtiDefinitionRepository.class, HbtiScoringEngine.class,
        AccountRegistrationService.class, IdentityCredentialConfig.class, TimeConfig.class})
abstract class WeeklyReviewIntegrationTestSupport {
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired protected WeeklyReviewService reviews;
    @Autowired protected DailyTrackingService tracking;
    @Autowired private WeightPlanService plans;
    @Autowired private AccountRegistrationService registration;
    @Autowired private ProfileService profiles;
    @Autowired private HbtiAssessmentService assessments;

    protected String activeUser(String prefix) {
        int sequence = SEQUENCE.incrementAndGet();
        String userId = registration.register(new RegisterAccountCommand(
                prefix + "-" + sequence + "@example.com", "correct horse battery staple"
        )).id();
        profiles.save(userId, new SaveProfileCommand(
                LocalDate.of(1990, 1, 1), CalculationSex.FEMALE,
                165, 70, 60, ActivityLevel.MODERATE, "Asia/Hong_Kong"
        ));
        profiles.screen(userId, new SafetyScreeningAnswers(false, false, false, false, false));
        List<HbtiAnswer> answers = IntStream.rangeClosed(1, 16)
                .mapToObj(index -> new HbtiAnswer("q" + index, 3)).toList();
        assessments.submit(userId, "assessment-" + sequence,
                new SubmitHbtiAssessmentCommand("1.0.0", answers));
        WeightPlanVersion draft = plans.createDraft(userId, "draft-" + sequence, WeightGoal.LOSS);
        plans.validate(userId, draft.planId(), draft.id());
        plans.confirm(userId, draft.planId(), draft.id());
        plans.activate(userId, draft.planId(), draft.id(), "activate-" + sequence);
        return userId;
    }
}

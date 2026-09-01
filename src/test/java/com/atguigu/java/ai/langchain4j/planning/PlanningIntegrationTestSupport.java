package com.atguigu.java.ai.langchain4j.planning;

import com.atguigu.java.ai.langchain4j.assessment.HbtiAnswer;
import com.atguigu.java.ai.langchain4j.assessment.HbtiAssessmentService;
import com.atguigu.java.ai.langchain4j.assessment.SubmitHbtiAssessmentCommand;
import com.atguigu.java.ai.langchain4j.identity.AccountRegistrationService;
import com.atguigu.java.ai.langchain4j.identity.RegisterAccountCommand;
import com.atguigu.java.ai.langchain4j.profile.ActivityLevel;
import com.atguigu.java.ai.langchain4j.profile.CalculationSex;
import com.atguigu.java.ai.langchain4j.profile.ProfileService;
import com.atguigu.java.ai.langchain4j.profile.SafetyScreeningAnswers;
import com.atguigu.java.ai.langchain4j.profile.SaveProfileCommand;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

abstract class PlanningIntegrationTestSupport {

    private static final AtomicInteger USER_SEQUENCE = new AtomicInteger();

    @Autowired
    protected WeightPlanService service;

    @Autowired
    private AccountRegistrationService registrationService;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private HbtiAssessmentService assessmentService;

    protected String eligibleUser(String prefix) {
        String email = prefix + "-" + USER_SEQUENCE.incrementAndGet() + "@example.com";
        String userId = registrationService.register(new RegisterAccountCommand(
                email, "correct horse battery staple"
        )).id();
        profileService.save(userId, new SaveProfileCommand(
                LocalDate.of(1990, 1, 1), CalculationSex.FEMALE,
                165, 70, 60, ActivityLevel.MODERATE, "Asia/Hong_Kong"
        ));
        profileService.screen(userId, new SafetyScreeningAnswers(
                false, false, false, false, false
        ));
        List<HbtiAnswer> answers = IntStream.rangeClosed(1, 16)
                .mapToObj(index -> new HbtiAnswer("q" + index, 3))
                .toList();
        assessmentService.submit(
                userId, "assessment-" + USER_SEQUENCE.incrementAndGet(),
                new SubmitHbtiAssessmentCommand("1.0.0", answers)
        );
        return userId;
    }

    protected WeightPlanVersion confirmedVersion(String userId, WeightGoal goal) {
        WeightPlanVersion draft = service.createDraft(userId, nextKey("draft"), goal);
        service.validate(userId, draft.planId(), draft.id());
        return service.confirm(userId, draft.planId(), draft.id());
    }

    protected void changeProfileWeight(String userId, double weightKg) {
        profileService.save(userId, new SaveProfileCommand(
                LocalDate.of(1990, 1, 1), CalculationSex.FEMALE,
                165, weightKg, 60, ActivityLevel.MODERATE, "Asia/Hong_Kong"
        ));
    }

    protected String nextKey(String prefix) {
        return prefix + "-" + USER_SEQUENCE.incrementAndGet();
    }
}

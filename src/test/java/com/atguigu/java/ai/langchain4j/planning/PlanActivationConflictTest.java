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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@MybatisTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
@Import({WeightPlanService.class, HealthCalculator.class, TargetRangePolicy.class,
        PlanningEligibilityPolicy.class, ProfileService.class, SafetyScreeningPolicy.class,
        HbtiAssessmentService.class, HbtiDefinitionRepository.class, HbtiScoringEngine.class,
        AccountRegistrationService.class, IdentityCredentialConfig.class, TimeConfig.class})
class PlanActivationConflictTest extends PlanningIntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentActivationLeavesExactlyOneActiveVersion() throws Exception {
        String userId = eligibleUser("plan-concurrent");
        WeightPlanVersion first = confirmedVersion(userId, WeightGoal.LOSS);
        WeightPlanVersion second = confirmedVersion(userId, WeightGoal.MAINTENANCE);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<WeightPlanVersion>> futures = List.of(
                    executor.submit(() -> activateAfter(start, userId, first)),
                    executor.submit(() -> activateAfter(start, userId, second))
            );
            start.countDown();
            for (Future<WeightPlanVersion> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        Integer activeCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM weight_plan_version WHERE plan_id = ? AND status = 'ACTIVE'",
                Integer.class, first.planId()
        );
        assertThat(activeCount).isEqualTo(1);
        assertThat(service.currentActive(userId)).isPresent();
    }

    private WeightPlanVersion activateAfter(
            CountDownLatch start,
            String userId,
            WeightPlanVersion version
    ) throws InterruptedException {
        start.await();
        return service.activate(userId, version.planId(), version.id(), nextKey("activate"));
    }
}

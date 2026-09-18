package com.atguigu.java.ai.langchain4j.tracking;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DailyTrackingOwnershipTest extends TrackingIntegrationTestSupport {

    @Test
    void summariesAreScopedToTheAuthenticatedOwner() {
        String owner = user("tracking-owner");
        String other = user("tracking-other");
        LocalDate date = LocalDate.now();
        service.recordNutrition(owner, "nutrition-owner", new NutritionCommand(
                date, 2000, new BigDecimal("120"),
                new BigDecimal("220"), new BigDecimal("70")
        ));

        assertThat(service.summary(owner, date).nutrition()).isPresent();
        assertThat(service.summary(other, date).nutrition()).isEmpty();
    }
}

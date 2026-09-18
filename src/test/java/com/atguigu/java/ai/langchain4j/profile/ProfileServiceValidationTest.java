package com.atguigu.java.ai.langchain4j.profile;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ProfileServiceValidationTest {

    private final ProfileService service = new ProfileService(
            mock(ProfileMapper.class),
            new SafetyScreeningPolicy(),
            Clock.fixed(Instant.parse("2026-08-14T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void rejectsDateOfBirthThatIsNotBeforeToday() {
        assertThatThrownBy(() -> service.save("user-1", command(
                LocalDate.of(2026, 8, 14), 170, "Asia/Hong_Kong"
        ))).isInstanceOf(InvalidProfileException.class);
    }

    @Test
    void rejectsNonFiniteMeasurements() {
        assertThatThrownBy(() -> service.save("user-1", command(
                LocalDate.of(1990, 1, 1), Double.NaN, "Asia/Hong_Kong"
        ))).isInstanceOf(InvalidProfileException.class);
    }

    @Test
    void rejectsMalformedTimeZone() {
        assertThatThrownBy(() -> service.save("user-1", command(
                LocalDate.of(1990, 1, 1), 170, ""
        ))).isInstanceOf(InvalidProfileException.class);
    }

    private SaveProfileCommand command(LocalDate dateOfBirth, double heightCm, String timeZone) {
        return new SaveProfileCommand(
                dateOfBirth, CalculationSex.FEMALE, heightCm,
                70, 60, ActivityLevel.MODERATE, timeZone
        );
    }
}

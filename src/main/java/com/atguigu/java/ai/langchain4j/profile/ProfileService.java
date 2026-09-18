package com.atguigu.java.ai.langchain4j.profile;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    private final ProfileMapper profileMapper;
    private final SafetyScreeningPolicy screeningPolicy;
    private final Clock clock;

    public ProfileService(ProfileMapper profileMapper, SafetyScreeningPolicy screeningPolicy, Clock clock) {
        this.profileMapper = profileMapper;
        this.screeningPolicy = screeningPolicy;
        this.clock = clock;
    }

    @Transactional
    public UserProfile save(String userId, SaveProfileCommand command) {
        validate(userId, command);
        Instant now = now();
        UserProfile existing = profileMapper.findByUserId(userId).orElse(null);
        UserProfile profile = new UserProfile(
                userId,
                command.dateOfBirth(),
                command.calculationSex(),
                command.heightCm(),
                command.currentWeightKg(),
                command.targetWeightKg(),
                command.activityLevel(),
                command.timeZone(),
                existing == null ? 0 : existing.screeningVersion(),
                existing == null ? now : existing.createdAt(),
                now
        );
        profileMapper.upsert(profile);
        return profile;
    }

    public Optional<UserProfile> find(String userId) {
        return profileMapper.findByUserId(userId);
    }

    public UserProfile getRequired(String userId) {
        return find(userId).orElseThrow(ProfileNotFoundException::new);
    }

    @Transactional
    public SafetyScreening screen(String userId, SafetyScreeningAnswers answers) {
        UserProfile profile = profileMapper.findByUserIdForUpdate(userId)
                .orElseThrow(ProfileRequiredException::new);
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(profile.timeZone())));
        ScreeningDecision decision = screeningPolicy.evaluate(profile, answers, today);
        int nextVersion = profile.screeningVersion() + 1;
        Instant now = now();
        String reasonCodes = decision.reasonCodes().stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
        SafetyScreening screening = new SafetyScreening(
                UUID.randomUUID().toString(), userId, nextVersion,
                answers.pregnantOrBreastfeeding(), answers.eatingDisorderHistory(),
                answers.medicalGuidanceRequired(), answers.weightAffectingMedication(),
                answers.concerningSymptoms(), decision.status(),
                decision.automaticPlanningAllowed(), reasonCodes, now
        );
        profileMapper.insertScreening(screening);
        if (profileMapper.advanceScreeningVersion(
                userId, profile.screeningVersion(), nextVersion, now
        ) != 1) {
            throw new IllegalStateException("Screening version conflict");
        }
        return screening;
    }

    public Optional<SafetyScreening> findCurrentScreening(String userId) {
        return profileMapper.findCurrentScreening(userId);
    }

    private void validate(String userId, SaveProfileCommand command) {
        if (userId == null || userId.isBlank() || command == null) {
            throw new InvalidProfileException("userId and profile are required");
        }
        if (command.dateOfBirth() == null || !command.dateOfBirth().isBefore(LocalDate.now(clock))) {
            throw new InvalidProfileException("dateOfBirth must be in the past");
        }
        if (command.calculationSex() == null || command.activityLevel() == null) {
            throw new InvalidProfileException("calculation inputs are required");
        }
        if (!Double.isFinite(command.heightCm())
                || !Double.isFinite(command.currentWeightKg())
                || !Double.isFinite(command.targetWeightKg())
                || command.heightCm() < 100 || command.heightCm() > 250
                || command.currentWeightKg() < 30 || command.currentWeightKg() > 350
                || command.targetWeightKg() < 30 || command.targetWeightKg() > 350) {
            throw new InvalidProfileException("profile measurements are outside supported bounds");
        }
        try {
            ZoneId.of(command.timeZone());
        } catch (NullPointerException | DateTimeException exception) {
            throw new InvalidProfileException("timeZone must be a valid IANA zone", exception);
        }
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }
}

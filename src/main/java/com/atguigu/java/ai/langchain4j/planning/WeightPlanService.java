package com.atguigu.java.ai.langchain4j.planning;

import com.atguigu.java.ai.langchain4j.assessment.HbtiAssessmentResult;
import com.atguigu.java.ai.langchain4j.assessment.HbtiAssessmentService;
import com.atguigu.java.ai.langchain4j.profile.SafetyScreening;
import com.atguigu.java.ai.langchain4j.profile.UserProfile;
import com.atguigu.java.ai.langchain4j.profile.ProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.HexFormat;

@Service
public class WeightPlanService {

    private static final int MAX_IDEMPOTENCY_KEY_BYTES = 128;

    private final WeightPlanMapper mapper;
    private final ProfileService profiles;
    private final HbtiAssessmentService assessments;
    private final PlanningEligibilityPolicy eligibilityPolicy;
    private final HealthCalculator healthCalculator;
    private final TargetRangePolicy targetRangePolicy;
    private final Clock clock;

    public WeightPlanService(
            WeightPlanMapper mapper,
            ProfileService profiles,
            HbtiAssessmentService assessments,
            PlanningEligibilityPolicy eligibilityPolicy,
            HealthCalculator healthCalculator,
            TargetRangePolicy targetRangePolicy,
            Clock clock
    ) {
        this.mapper = mapper;
        this.profiles = profiles;
        this.assessments = assessments;
        this.eligibilityPolicy = eligibilityPolicy;
        this.healthCalculator = healthCalculator;
        this.targetRangePolicy = targetRangePolicy;
        this.clock = clock;
    }

    @Transactional
    public WeightPlanVersion createDraft(String userId, String idempotencyKey, WeightGoal goal) {
        requireUserAndGoal(userId, goal);
        String keyHash = idempotencyKeyHash(idempotencyKey);
        lockUser(userId);
        Optional<WeightPlanVersion> existing = mapper.findByDraftKey(userId, keyHash);
        if (existing.isPresent()) {
            if (existing.get().goal() != goal) {
                throw new PlanIdempotencyConflictException();
            }
            return existing.get();
        }
        PlanningInputs inputs = currentInputs(userId);
        Instant now = now();
        WeightPlanRow plan = mapper.findPlanForUpdate(userId).orElse(null);
        String planId;
        int versionNo;
        if (plan == null) {
            planId = UUID.randomUUID().toString();
            versionNo = 1;
            mapper.insertPlan(planId, userId, now);
        } else {
            planId = plan.id();
            versionNo = plan.nextVersionNo();
            if (mapper.advanceVersion(planId, userId, versionNo + 1, now) != 1) {
                throw new IllegalStateException("Plan version sequence update failed");
            }
        }

        HealthCalculation calculation = calculate(inputs.profile());
        TargetRange range = targetRangePolicy.propose(
                goal, calculation.bmrKcalPerDay(), calculation.tdeeKcalPerDay()
        );
        WeightPlanVersion version = new WeightPlanVersion(
                UUID.randomUUID().toString(), planId, versionNo, PlanVersionStatus.DRAFT, goal,
                inputs.profile().updatedAt(), inputs.screening().id(), inputs.screening().version(),
                inputs.assessment().id(), calculation.formulaVersion(), range.policyVersion(),
                calculation.bmi(), calculation.bmrKcalPerDay(), calculation.tdeeKcalPerDay(),
                range.energyMinKcalPerDay(), range.energyMaxKcalPerDay(),
                range.weeklyWeightChangeMinPercent(), range.weeklyWeightChangeMaxPercent(),
                now, null, null, null, null
        );
        mapper.insertVersion(version, keyHash);
        return version;
    }

    @Transactional
    public WeightPlanVersion validate(String userId, String planId, String versionId) {
        return transition(userId, planId, versionId,
                PlanVersionStatus.DRAFT, PlanVersionStatus.VALIDATED);
    }

    @Transactional
    public WeightPlanVersion confirm(String userId, String planId, String versionId) {
        return transition(userId, planId, versionId,
                PlanVersionStatus.VALIDATED, PlanVersionStatus.CONFIRMED);
    }

    @Transactional
    public WeightPlanVersion activate(
            String userId,
            String planId,
            String versionId,
            String idempotencyKey
    ) {
        String keyHash = idempotencyKeyHash(idempotencyKey);
        lockUser(userId);
        Optional<WeightPlanVersion> replay = mapper.findByActivationKey(userId, keyHash);
        if (replay.isPresent()) {
            if (!replay.get().planId().equals(planId) || !replay.get().id().equals(versionId)) {
                throw new PlanIdempotencyConflictException();
            }
            return replay.get();
        }
        WeightPlanVersion candidate = requiredVersion(userId, planId, versionId);
        requireStatus(candidate, PlanVersionStatus.CONFIRMED);
        assertCurrent(candidate, currentInputs(userId));
        Instant now = now();

        mapper.findActive(userId).ifPresent(active -> {
            if (mapper.transition(
                    userId, active.planId(), active.id(), PlanVersionStatus.ACTIVE,
                    PlanVersionStatus.REPLACED, null, null, null, now, null
            ) != 1) {
                throw new IllegalStateException("Active plan replacement conflict");
            }
        });
        if (mapper.transition(
                userId, planId, versionId, PlanVersionStatus.CONFIRMED,
                PlanVersionStatus.ACTIVE, null, null, now, null, keyHash
        ) != 1 || mapper.setActiveVersion(planId, userId, versionId, now) != 1) {
            throw new IllegalStateException("Plan activation conflict");
        }
        return requiredVersion(userId, planId, versionId);
    }

    @Transactional(readOnly = true)
    public WeightPlanVersion get(String userId, String planId, String versionId) {
        return requiredVersion(userId, planId, versionId);
    }

    @Transactional(readOnly = true)
    public Optional<WeightPlanVersion> currentActive(String userId) {
        return mapper.findActive(userId);
    }

    private WeightPlanVersion transition(
            String userId,
            String planId,
            String versionId,
            PlanVersionStatus expected,
            PlanVersionStatus next
    ) {
        lockUser(userId);
        WeightPlanVersion version = requiredVersion(userId, planId, versionId);
        requireStatus(version, expected);
        assertCurrent(version, currentInputs(userId));
        Instant now = now();
        int changed = mapper.transition(
                userId, planId, versionId, expected, next,
                next == PlanVersionStatus.VALIDATED ? now : null,
                next == PlanVersionStatus.CONFIRMED ? now : null,
                null, null, null
        );
        if (changed != 1) {
            throw new IllegalStateException("Plan transition conflict");
        }
        return requiredVersion(userId, planId, versionId);
    }

    private void assertCurrent(WeightPlanVersion version, PlanningInputs current) {
        HealthCalculation calculation = calculate(current.profile());
        TargetRange range = targetRangePolicy.propose(
                version.goal(), calculation.bmrKcalPerDay(), calculation.tdeeKcalPerDay()
        );
        boolean unchanged = version.profileUpdatedAt().equals(current.profile().updatedAt())
                && version.screeningId().equals(current.screening().id())
                && version.screeningVersion() == current.screening().version()
                && version.assessmentAttemptId().equals(current.assessment().id())
                && version.formulaVersion().equals(calculation.formulaVersion())
                && version.targetPolicyVersion().equals(range.policyVersion())
                && same(version.bmi(), calculation.bmi())
                && version.bmrKcalPerDay() == calculation.bmrKcalPerDay()
                && version.tdeeKcalPerDay() == calculation.tdeeKcalPerDay()
                && version.energyMinKcalPerDay() == range.energyMinKcalPerDay()
                && version.energyMaxKcalPerDay() == range.energyMaxKcalPerDay()
                && same(version.weeklyWeightChangeMinPercent(), range.weeklyWeightChangeMinPercent())
                && same(version.weeklyWeightChangeMaxPercent(), range.weeklyWeightChangeMaxPercent());
        if (!unchanged) {
            throw new PlanningPrerequisiteException(PlanningPrerequisiteReason.PLAN_INPUTS_CHANGED);
        }
    }

    private PlanningInputs currentInputs(String userId) {
        UserProfile profile = profiles.find(userId).orElseThrow(() ->
                new PlanningPrerequisiteException(PlanningPrerequisiteReason.PROFILE_REQUIRED));
        SafetyScreening screening = profiles.findCurrentScreening(userId).orElseThrow(() ->
                new PlanningPrerequisiteException(PlanningPrerequisiteReason.SCREENING_REQUIRED));
        PlanningEligibility eligibility = eligibilityPolicy.evaluate(profile, screening);
        if (!eligibility.allowed()) {
            throw new PlanningPrerequisiteException(mapReason(eligibility.reason()));
        }
        HbtiAssessmentResult assessment = assessments.current(userId).orElseThrow(() ->
                new PlanningPrerequisiteException(PlanningPrerequisiteReason.ASSESSMENT_REQUIRED));
        return new PlanningInputs(profile, screening, assessment);
    }

    private PlanningPrerequisiteReason mapReason(PlanningEligibilityReason reason) {
        return switch (reason) {
            case SCREENING_REQUIRED -> PlanningPrerequisiteReason.SCREENING_REQUIRED;
            case SCREENING_STALE -> PlanningPrerequisiteReason.SCREENING_STALE;
            case PROFESSIONAL_REVIEW_REQUIRED ->
                    PlanningPrerequisiteReason.PROFESSIONAL_REVIEW_REQUIRED;
            case NOT_ELIGIBLE -> PlanningPrerequisiteReason.NOT_ELIGIBLE;
            case ELIGIBLE -> throw new IllegalArgumentException("Eligible is not a rejection reason");
        };
    }

    private HealthCalculation calculate(UserProfile profile) {
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(profile.timeZone())));
        int age = Period.between(profile.dateOfBirth(), today).getYears();
        return healthCalculator.calculate(new HealthCalculationInput(
                age, profile.calculationSex(), profile.heightCm(),
                profile.currentWeightKg(), profile.activityLevel()
        ));
    }

    private void lockUser(String userId) {
        if (userId == null || userId.isBlank() || mapper.lockUser(userId) == null) {
            throw new PlanVersionNotFoundException();
        }
    }

    private void requireUserAndGoal(String userId, WeightGoal goal) {
        if (userId == null || userId.isBlank() || goal == null) {
            throw new InvalidPlanRequestException("Authenticated user and goal are required");
        }
    }

    private String idempotencyKeyHash(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.getBytes(StandardCharsets.UTF_8).length > MAX_IDEMPOTENCY_KEY_BYTES) {
            throw new InvalidPlanRequestException("Idempotency key is invalid");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(idempotencyKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private WeightPlanVersion requiredVersion(String userId, String planId, String versionId) {
        if (planId == null || planId.isBlank() || versionId == null || versionId.isBlank()) {
            throw new PlanVersionNotFoundException();
        }
        return mapper.findVersion(userId, planId, versionId)
                .orElseThrow(PlanVersionNotFoundException::new);
    }

    private void requireStatus(WeightPlanVersion version, PlanVersionStatus expected) {
        if (version.status() != expected) {
            throw new InvalidPlanTransitionException(expected, version.status());
        }
    }

    private boolean same(BigDecimal left, BigDecimal right) {
        return left.compareTo(right) == 0;
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    private record PlanningInputs(
            UserProfile profile,
            SafetyScreening screening,
            HbtiAssessmentResult assessment
    ) {
    }
}

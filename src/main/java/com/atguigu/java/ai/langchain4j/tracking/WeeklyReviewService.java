package com.atguigu.java.ai.langchain4j.tracking;

import com.atguigu.java.ai.langchain4j.planning.WeightPlanService;
import com.atguigu.java.ai.langchain4j.planning.WeightPlanVersion;
import com.atguigu.java.ai.langchain4j.profile.ProfileService;
import com.atguigu.java.ai.langchain4j.profile.UserProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WeeklyReviewService {
    private static final int WINDOW_DAYS = 7;
    private static final int TRACKING_HISTORY_DAYS = 90;

    private final WeeklyReviewMapper mapper;
    private final ProfileService profiles;
    private final WeightPlanService plans;
    private final WeeklyReviewPolicy policy;
    private final Clock clock;

    public WeeklyReviewService(WeeklyReviewMapper mapper, ProfileService profiles,
                               WeightPlanService plans, WeeklyReviewPolicy policy, Clock clock) {
        this.mapper = mapper;
        this.profiles = profiles;
        this.plans = plans;
        this.policy = policy;
        this.clock = clock;
    }

    @Transactional
    public WeeklyReviewWrite generate(String userId, LocalDate windowEnd) {
        validateRequest(userId, windowEnd);
        LocalDate windowStart = windowEnd.minusDays(WINDOW_DAYS - 1L);
        if (mapper.lockUser(userId) == null) {
            throw new InvalidWeeklyReviewException("User is invalid");
        }
        WeightPlanVersion plan = plans.currentActive(userId).orElseThrow(() ->
                new WeeklyReviewPrerequisiteException("An active plan is required"));
        List<WeeklyMetricFact> metrics = mapper.metrics(userId, windowStart, windowEnd);
        List<WeeklyNutritionFact> nutrition = mapper.nutrition(userId, windowStart, windowEnd);
        List<WeeklyTrainingFact> training = mapper.training(userId, windowStart, windowEnd);
        String inputHash = inputHash(plan, windowStart, windowEnd, metrics, nutrition, training);
        Optional<WeeklyReview> replay = mapper.findByInput(userId, windowEnd, inputHash);
        if (replay.isPresent()) return new WeeklyReviewWrite(replay.get(), true);

        WeeklyReviewInput input = new WeeklyReviewInput(
                windowStart, windowEnd, plan.goal(), plan.energyMinKcalPerDay(),
                plan.energyMaxKcalPerDay(), plan.weeklyWeightChangeMinPercent(),
                plan.weeklyWeightChangeMaxPercent(), metrics.stream()
                .filter(value -> value.weightKg() != null)
                .map(value -> new WeightObservation(value.localDate(), value.weightKg())).toList(),
                nutrition.stream().map(WeeklyNutritionFact::energyKcal).toList(),
                metrics.stream().filter(value -> value.steps() != null)
                        .map(WeeklyMetricFact::steps).toList(),
                metrics.stream().filter(value -> value.sleepMinutes() != null)
                        .map(WeeklyMetricFact::sleepMinutes).toList(),
                training.stream().mapToInt(WeeklyTrainingFact::durationMinutes).sum(),
                (int) training.stream().map(WeeklyTrainingFact::localDate).distinct().count()
        );
        WeeklyReviewAnalysis analysis = policy.analyze(input);
        WeeklyReview review = toReview(plan, windowStart, windowEnd,
                mapper.nextVersion(userId, windowEnd), analysis);
        if (mapper.insert(userId, inputHash, review) != 1) {
            throw new IllegalStateException("Weekly review insert failed");
        }
        return new WeeklyReviewWrite(review, false);
    }

    @Transactional(readOnly = true)
    public Optional<WeeklyReview> get(String userId, String reviewId) {
        if (userId == null || userId.isBlank() || reviewId == null || reviewId.isBlank()) {
            return Optional.empty();
        }
        return mapper.findById(userId, reviewId);
    }

    private void validateRequest(String userId, LocalDate windowEnd) {
        if (userId == null || userId.isBlank() || windowEnd == null) {
            throw new InvalidWeeklyReviewException("User and window end are required");
        }
        UserProfile profile = profiles.find(userId).orElseThrow(() ->
                new WeeklyReviewPrerequisiteException("A profile is required"));
        LocalDate today;
        try {
            today = LocalDate.now(clock.withZone(ZoneId.of(profile.timeZone())));
        } catch (RuntimeException exception) {
            throw new InvalidWeeklyReviewException("Profile time zone is invalid");
        }
        LocalDate start = windowEnd.minusDays(WINDOW_DAYS - 1L);
        if (windowEnd.isAfter(today) || start.isBefore(today.minusDays(TRACKING_HISTORY_DAYS))) {
            throw new InvalidWeeklyReviewException("Review window is outside tracking history");
        }
    }

    private WeeklyReview toReview(WeightPlanVersion plan, LocalDate start, LocalDate end,
                                  int version, WeeklyReviewAnalysis value) {
        return new WeeklyReview(
                UUID.randomUUID().toString(), plan.id(), start, end, version,
                WeeklyReviewPolicy.POLICY_VERSION, value.weightObservationDays(),
                value.nutritionLoggedDays(), value.stepsObservedDays(), value.sleepObservedDays(),
                value.trainingDays(), value.averageWeightKg(), value.weightTrendPercent(),
                value.nutritionAdherencePercent(), value.averageSteps(),
                value.averageSleepMinutes(), value.totalTrainingMinutes(),
                value.recommendation(), value.proposedEnergyDeltaKcalPerDay(), value.reason(), now()
        );
    }

    private String inputHash(WeightPlanVersion plan, LocalDate start, LocalDate end,
                             List<WeeklyMetricFact> metrics,
                             List<WeeklyNutritionFact> nutrition,
                             List<WeeklyTrainingFact> training) {
        StringBuilder canonical = new StringBuilder(WeeklyReviewPolicy.POLICY_VERSION)
                .append('|').append(plan.id()).append('|').append(plan.goal())
                .append('|').append(plan.energyMinKcalPerDay())
                .append('|').append(plan.energyMaxKcalPerDay())
                .append('|').append(plan.weeklyWeightChangeMinPercent())
                .append('|').append(plan.weeklyWeightChangeMaxPercent())
                .append('|').append(start).append('|').append(end);
        metrics.forEach(value -> canonical.append("|M:").append(value.localDate())
                .append(':').append(value.weightKg()).append(':').append(value.steps())
                .append(':').append(value.sleepMinutes()));
        nutrition.forEach(value -> canonical.append("|N:").append(value.localDate())
                .append(':').append(value.energyKcal()));
        training.forEach(value -> canonical.append("|T:").append(value.localDate())
                .append(':').append(value.durationMinutes()));
        return sha256(canonical.toString());
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private Instant now() { return clock.instant().truncatedTo(ChronoUnit.MICROS); }
}

package com.atguigu.java.ai.langchain4j.tracking;

import com.atguigu.java.ai.langchain4j.profile.ProfileService;
import com.atguigu.java.ai.langchain4j.profile.UserProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
public class DailyTrackingService {
    private static final int MAX_KEY_BYTES = 128;
    private static final int HISTORY_DAYS = 90;

    private final DailyTrackingMapper mapper;
    private final ProfileService profiles;
    private final Clock clock;

    public DailyTrackingService(DailyTrackingMapper mapper, ProfileService profiles, Clock clock) {
        this.mapper = mapper;
        this.profiles = profiles;
        this.clock = clock;
    }

    @Transactional
    public TrackingWrite<DailyMetric> recordMetric(
            String userId, String idempotencyKey, DailyMetricCommand command
    ) {
        requireUser(userId);
        validateMetric(command);
        validateDate(userId, command.localDate());
        String keyHash = keyHash(idempotencyKey);
        DailyMetric normalized = new DailyMetric(
                UUID.randomUUID().toString(), command.localDate(), decimal(command.weightKg(), 2),
                command.steps(), command.activityMinutes(), command.sleepMinutes(),
                command.sleepQuality(), now()
        );
        String payloadHash = sha256(canonical(normalized));
        lockUser(userId);
        Optional<DailyMetricRow> replay = mapper.metricByKey(userId, keyHash);
        if (replay.isPresent()) {
            verifyPayload(replay.get().payloadHash(), payloadHash);
            return new TrackingWrite<>(replay.get().domain(), true);
        }
        if (mapper.metricByDate(userId, command.localDate()).isPresent()) {
            throw new TrackingDateConflictException();
        }
        mapper.insertMetric(userId, keyHash, payloadHash, normalized);
        return new TrackingWrite<>(normalized, false);
    }

    @Transactional
    public TrackingWrite<NutritionLog> recordNutrition(
            String userId, String idempotencyKey, NutritionCommand command
    ) {
        requireUser(userId);
        validateNutrition(command);
        validateDate(userId, command.localDate());
        String keyHash = keyHash(idempotencyKey);
        NutritionLog normalized = new NutritionLog(
                UUID.randomUUID().toString(), command.localDate(), command.energyKcal(),
                decimal(command.proteinG(), 1), decimal(command.carbohydrateG(), 1),
                decimal(command.fatG(), 1), now()
        );
        String payloadHash = sha256(canonical(normalized));
        lockUser(userId);
        Optional<NutritionRow> replay = mapper.nutritionByKey(userId, keyHash);
        if (replay.isPresent()) {
            verifyPayload(replay.get().payloadHash(), payloadHash);
            return new TrackingWrite<>(replay.get().domain(), true);
        }
        if (mapper.nutritionByDate(userId, command.localDate()).isPresent()) {
            throw new TrackingDateConflictException();
        }
        mapper.insertNutrition(userId, keyHash, payloadHash, normalized);
        return new TrackingWrite<>(normalized, false);
    }

    @Transactional
    public TrackingWrite<TrainingLog> recordTraining(
            String userId, String idempotencyKey, TrainingCommand command
    ) {
        requireUser(userId);
        validateTraining(command);
        validateDate(userId, command.localDate());
        String keyHash = keyHash(idempotencyKey);
        TrainingLog record = new TrainingLog(
                UUID.randomUUID().toString(), command.localDate(), command.trainingType(),
                command.durationMinutes(), command.intensity(), now()
        );
        String payloadHash = sha256(canonical(record));
        lockUser(userId);
        Optional<TrainingRow> replay = mapper.trainingByKey(userId, keyHash);
        if (replay.isPresent()) {
            verifyPayload(replay.get().payloadHash(), payloadHash);
            return new TrackingWrite<>(replay.get().domain(), true);
        }
        mapper.insertTraining(userId, keyHash, payloadHash, record);
        return new TrackingWrite<>(record, false);
    }

    @Transactional(readOnly = true)
    public DailySummary summary(String userId, LocalDate date) {
        requireUser(userId);
        validateDate(userId, date);
        Optional<DailyMetric> metric = mapper.metricByDate(userId, date).map(DailyMetricRow::domain);
        Optional<NutritionLog> nutrition = mapper.nutritionByDate(userId, date).map(NutritionRow::domain);
        List<TrainingLog> training = mapper.trainingByDate(userId, date).stream()
                .map(TrainingRow::domain).toList();
        int minutes = training.stream().mapToInt(TrainingLog::durationMinutes).sum();
        return new DailySummary(date, metric, nutrition, training, minutes);
    }

    private void validateMetric(DailyMetricCommand value) {
        if (value == null || allNull(value.weightKg(), value.steps(), value.activityMinutes(),
                value.sleepMinutes(), value.sleepQuality())
                || outsideOptional(value.weightKg(), "30", "350")
                || outside(value.steps(), 0, 100000)
                || outside(value.activityMinutes(), 0, 1440)
                || outside(value.sleepMinutes(), 0, 1440) || outside(value.sleepQuality(), 1, 5)) {
            throw new InvalidTrackingRequestException("Daily metric values are invalid");
        }
    }

    private void validateNutrition(NutritionCommand value) {
        if (value == null || value.energyKcal() < 0 || value.energyKcal() > 10000
                || outside(value.proteinG(), "0", "1000")
                || outside(value.carbohydrateG(), "0", "1000")
                || outside(value.fatG(), "0", "1000")) {
            throw new InvalidTrackingRequestException("Nutrition values are invalid");
        }
    }

    private void validateTraining(TrainingCommand value) {
        if (value == null || value.trainingType() == null || value.intensity() == null
                || value.durationMinutes() < 1 || value.durationMinutes() > 600) {
            throw new InvalidTrackingRequestException("Training values are invalid");
        }
    }

    private void validateDate(String userId, LocalDate date) {
        if (date == null) throw new InvalidTrackingRequestException("Tracking date is required");
        UserProfile profile = profiles.find(userId)
                .orElseThrow(() -> new InvalidTrackingRequestException("Profile is required"));
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(profile.timeZone())));
        if (date.isAfter(today) || date.isBefore(today.minusDays(HISTORY_DAYS))) {
            throw new InvalidTrackingRequestException("Tracking date is outside the supported window");
        }
    }

    private void requireUser(String userId) {
        if (userId == null || userId.isBlank()) throw new InvalidTrackingRequestException("User is required");
    }

    private void lockUser(String userId) {
        if (mapper.lockUser(userId) == null) throw new InvalidTrackingRequestException("User is invalid");
    }

    private String keyHash(String key) {
        if (key == null || key.isBlank() || key.getBytes(StandardCharsets.UTF_8).length > MAX_KEY_BYTES) {
            throw new InvalidTrackingRequestException("Idempotency key is invalid");
        }
        return sha256(key);
    }

    private void verifyPayload(String existing, String requested) {
        if (!existing.equals(requested)) throw new TrackingIdempotencyConflictException();
    }

    private BigDecimal decimal(BigDecimal value, int scale) {
        return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP);
    }

    private boolean outside(BigDecimal value, String min, String max) {
        return value == null || value.compareTo(new BigDecimal(min)) < 0
                || value.compareTo(new BigDecimal(max)) > 0;
    }

    private boolean outsideOptional(BigDecimal value, String min, String max) {
        return value != null && outside(value, min, max);
    }

    private boolean outside(Integer value, int min, int max) {
        return value != null && (value < min || value > max);
    }

    private boolean allNull(Object... values) {
        for (Object value : values) if (value != null) return false;
        return true;
    }

    private String canonical(DailyMetric value) {
        return value.localDate() + "|" + value.weightKg() + "|" + value.steps() + "|"
                + value.activityMinutes() + "|" + value.sleepMinutes() + "|" + value.sleepQuality();
    }

    private String canonical(NutritionLog value) {
        return value.localDate() + "|" + value.energyKcal() + "|" + value.proteinG() + "|"
                + value.carbohydrateG() + "|" + value.fatG();
    }

    private String canonical(TrainingLog value) {
        return value.localDate() + "|" + value.trainingType() + "|"
                + value.durationMinutes() + "|" + value.intensity();
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

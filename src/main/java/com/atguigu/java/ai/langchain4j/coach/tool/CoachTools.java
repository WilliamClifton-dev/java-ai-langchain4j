package com.atguigu.java.ai.langchain4j.coach.tool;

import com.atguigu.java.ai.langchain4j.planning.WeightPlanService;
import com.atguigu.java.ai.langchain4j.tracking.DailyMetricCommand;
import com.atguigu.java.ai.langchain4j.tracking.DailyTrackingService;
import com.atguigu.java.ai.langchain4j.tracking.NutritionCommand;
import com.atguigu.java.ai.langchain4j.tracking.TrainingCommand;
import com.atguigu.java.ai.langchain4j.tracking.TrainingIntensity;
import com.atguigu.java.ai.langchain4j.tracking.TrainingType;
import com.atguigu.java.ai.langchain4j.tracking.WeeklyReviewService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.function.Function;

@Component("coachTools")
public class CoachTools {
    private final CoachToolContext context;
    private final WeightPlanService plans;
    private final DailyTrackingService tracking;
    private final WeeklyReviewService reviews;

    public CoachTools(CoachToolContext context, WeightPlanService plans,
                      DailyTrackingService tracking, WeeklyReviewService reviews) {
        this.context = context;
        this.plans = plans;
        this.tracking = tracking;
        this.reviews = reviews;
    }

    @Tool(name = "get_active_plan", value = "Read the authenticated user's active plan")
    public CoachToolResult<?> getActivePlan() {
        return read(invocation -> plans.currentActive(invocation.userId()).orElse(null));
    }

    @Tool(name = "get_daily_summary", value = "Read one owned local-date tracking summary")
    public CoachToolResult<?> getDailySummary(@P("ISO local date YYYY-MM-DD") String localDate) {
        return read(invocation -> tracking.summary(invocation.userId(), LocalDate.parse(localDate)));
    }

    @Tool(name = "get_weekly_review", value = "Read one owned deterministic weekly review")
    public CoachToolResult<?> getWeeklyReview(@P("Weekly review identifier") String reviewId) {
        return read(invocation -> reviews.get(invocation.userId(), reviewId).orElse(null));
    }

    @Tool(name = "record_daily_metric", value = "Record typed daily weight, activity, and sleep facts")
    public CoachToolResult<?> recordDailyMetric(
            @P("ISO local date YYYY-MM-DD") String localDate,
            @P("Weight in kilograms, optional") Double weightKg,
            @P("Steps, optional") Integer steps,
            @P("Activity minutes, optional") Integer activityMinutes,
            @P("Sleep minutes, optional") Integer sleepMinutes,
            @P("Sleep quality 1 to 5, optional") Integer sleepQuality
    ) {
        return write(invocation ->
                tracking.recordMetric(invocation.userId(), toolKey(invocation, "daily_metric",
                                localDate + '|' + weightKg + '|' + steps + '|' + activityMinutes
                                        + '|' + sleepMinutes + '|' + sleepQuality),
                        new DailyMetricCommand(LocalDate.parse(localDate), decimal(weightKg), steps,
                                activityMinutes, sleepMinutes, sleepQuality)));
    }

    @Tool(name = "record_nutrition", value = "Record one typed daily nutrition summary")
    public CoachToolResult<?> recordNutrition(
            @P("ISO local date YYYY-MM-DD") String localDate,
            @P("Energy in kcal") Integer energyKcal,
            @P("Protein in grams") Double proteinG,
            @P("Carbohydrate in grams") Double carbohydrateG,
            @P("Fat in grams") Double fatG
    ) {
        String payload = localDate + '|' + energyKcal + '|' + proteinG + '|'
                + carbohydrateG + '|' + fatG;
        return write(invocation -> tracking.recordNutrition(
                invocation.userId(), toolKey(invocation, "nutrition", payload),
                new NutritionCommand(LocalDate.parse(localDate), required(energyKcal),
                        requiredDecimal(proteinG), requiredDecimal(carbohydrateG),
                        requiredDecimal(fatG))));
    }

    @Tool(name = "record_training", value = "Record one typed training session")
    public CoachToolResult<?> recordTraining(
            @P("ISO local date YYYY-MM-DD") String localDate,
            @P("STRENGTH, CARDIO, MOBILITY, SPORT, or OTHER") String trainingType,
            @P("Duration from 1 to 600 minutes") Integer durationMinutes,
            @P("LOW, MODERATE, or HIGH") String intensity
    ) {
        String payload = localDate + '|' + trainingType + '|' + durationMinutes + '|' + intensity;
        return write(invocation -> tracking.recordTraining(
                invocation.userId(), toolKey(invocation, "training", payload),
                new TrainingCommand(LocalDate.parse(localDate), TrainingType.valueOf(trainingType),
                        required(durationMinutes), TrainingIntensity.valueOf(intensity))));
    }

    private CoachToolResult<?> read(Function<CoachToolContext.Invocation, Object> action) {
        return context.current().map(invocation -> {
            try {
                Object data = action.apply(invocation);
                return data == null ? CoachToolResult.failure("TOOL_NOT_FOUND")
                        : CoachToolResult.success(data);
            } catch (IllegalArgumentException exception) {
                return CoachToolResult.failure("TOOL_INVALID_ARGUMENT");
            } catch (RuntimeException exception) {
                return CoachToolResult.failure("TOOL_READ_FAILED");
            }
        }).orElseGet(() -> CoachToolResult.failure("TOOL_UNAUTHORIZED"));
    }

    private CoachToolResult<?> write(Function<CoachToolContext.Invocation, Object> action) {
        return context.current().map(invocation -> {
            try {
                Object committed = action.apply(invocation);
                return CoachToolResult.success(committed);
            } catch (IllegalArgumentException exception) {
                return CoachToolResult.failure("TOOL_INVALID_ARGUMENT");
            } catch (RuntimeException exception) {
                return CoachToolResult.failure("TOOL_WRITE_FAILED");
            }
        }).orElseGet(() -> CoachToolResult.failure("TOOL_UNAUTHORIZED"));
    }

    private String toolKey(CoachToolContext.Invocation invocation, String tool, String payload) {
        return "coach-tool:" + sha256(invocation.requestNonce() + '|' + tool + '|' + payload);
    }

    private BigDecimal decimal(Double value) {
        if (value == null) return null;
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Number is not finite");
        return BigDecimal.valueOf(value);
    }

    private BigDecimal requiredDecimal(Double value) {
        BigDecimal decimal = decimal(value);
        if (decimal == null) throw new IllegalArgumentException("Number is required");
        return decimal;
    }

    private int required(Integer value) {
        if (value == null) throw new IllegalArgumentException("Number is required");
        return value;
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}

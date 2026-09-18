package com.atguigu.java.ai.langchain4j.identity;

import com.atguigu.java.ai.langchain4j.identity.api.AccountDataExport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AccountDataExportService {
    private static final int MAX_ROWS = 1000;

    private final AccountDataLifecycleMapper mapper;
    private final Clock clock;

    public AccountDataExportService(AccountDataLifecycleMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AccountDataExport export(String userId) {
        AccountDataLifecycleMapper.AccountRow account = mapper.findAccount(userId)
                .orElseThrow(AccountDataNotFoundException::new);
        List<AccountDataLifecycleMapper.ScreeningRow> screenings = bounded(
                mapper.findScreenings(userId));
        List<AccountDataLifecycleMapper.AssessmentRow> assessments = bounded(
                mapper.findAssessments(userId));
        List<AccountDataLifecycleMapper.AnswerRow> answers = boundedNested(
                mapper.findAnswers(userId));
        List<AccountDataLifecycleMapper.ScoreRow> scores = boundedNested(
                mapper.findScores(userId));
        List<AccountDataLifecycleMapper.PlanRow> plans = bounded(mapper.findPlans(userId));
        List<AccountDataLifecycleMapper.PlanVersionRow> planVersions = bounded(
                mapper.findPlanVersions(userId));
        List<AccountDataLifecycleMapper.DailyMetricRow> dailyMetrics = bounded(
                mapper.findDailyMetrics(userId));
        List<AccountDataLifecycleMapper.NutritionRow> nutritionLogs = bounded(
                mapper.findNutritionLogs(userId));
        List<AccountDataLifecycleMapper.TrainingRow> trainingLogs = bounded(
                mapper.findTrainingLogs(userId));
        List<AccountDataLifecycleMapper.WeeklyReviewRow> reviews = bounded(
                mapper.findWeeklyReviews(userId));
        List<AccountDataLifecycleMapper.ConversationRow> conversations = bounded(
                mapper.findConversations(userId));
        List<AccountDataLifecycleMapper.MessageRow> messages = boundedNested(
                mapper.findMessages(userId));

        Map<String, List<AccountDataExport.Answer>> answersByAttempt = answers.stream()
                .collect(Collectors.groupingBy(AccountDataLifecycleMapper.AnswerRow::attemptId,
                        LinkedHashMap::new,
                        Collectors.mapping(row -> new AccountDataExport.Answer(
                                row.itemKey(), row.answerValue()), Collectors.toList())));
        Map<String, List<AccountDataExport.Score>> scoresByAttempt = scores.stream()
                .collect(Collectors.groupingBy(AccountDataLifecycleMapper.ScoreRow::attemptId,
                        LinkedHashMap::new,
                        Collectors.mapping(row -> new AccountDataExport.Score(
                                row.dimensionCode(), row.ordinal(), row.chosenPole(),
                                row.leftScore(), row.rightScore()), Collectors.toList())));
        Map<String, List<AccountDataExport.PlanVersion>> versionsByPlan = planVersions.stream()
                .collect(Collectors.groupingBy(AccountDataLifecycleMapper.PlanVersionRow::planId,
                        LinkedHashMap::new,
                        Collectors.mapping(this::planVersion, Collectors.toList())));
        Map<String, List<AccountDataExport.Message>> messagesByConversation = messages.stream()
                .collect(Collectors.groupingBy(AccountDataLifecycleMapper.MessageRow::conversationId,
                        LinkedHashMap::new,
                        Collectors.mapping(row -> new AccountDataExport.Message(
                                row.id(), row.sequenceNo(), row.messageJson(), row.createdAt()),
                                Collectors.toList())));

        return new AccountDataExport(
                "1",
                clock.instant(),
                new AccountDataExport.Account(account.id(), account.email(),
                        account.status().name(), account.createdAt(), account.updatedAt()),
                mapper.findProfile(userId).map(this::profile).orElse(null),
                screenings.stream().map(this::screening).toList(),
                assessments.stream().map(row -> new AccountDataExport.Assessment(
                        row.id(), row.definitionId(), row.status(), row.typeCode(),
                        row.createdAt(), row.completedAt(),
                        answersByAttempt.getOrDefault(row.id(), List.of()),
                        scoresByAttempt.getOrDefault(row.id(), List.of()))).toList(),
                plans.stream().map(row -> new AccountDataExport.Plan(
                        row.id(), row.activeVersionId(), row.nextVersionNo(), row.createdAt(),
                        row.updatedAt(), versionsByPlan.getOrDefault(row.id(), List.of()))).toList(),
                dailyMetrics.stream().map(row -> new AccountDataExport.DailyMetric(
                        row.id(), row.localDate(), row.weightKg(), row.steps(),
                        row.activityMinutes(), row.sleepMinutes(), row.sleepQuality(),
                        row.createdAt())).toList(),
                nutritionLogs.stream().map(row -> new AccountDataExport.Nutrition(
                        row.id(), row.localDate(), row.energyKcal(), row.proteinG(),
                        row.carbohydrateG(), row.fatG(), row.createdAt())).toList(),
                trainingLogs.stream().map(row -> new AccountDataExport.Training(
                        row.id(), row.localDate(), row.trainingType(), row.durationMinutes(),
                        row.intensity(), row.createdAt())).toList(),
                reviews.stream().map(row -> new AccountDataExport.WeeklyReview(
                        row.id(), row.planVersionId(), row.windowStart(), row.windowEnd(),
                        row.versionNo(), row.policyVersion(), row.weightObservationDays(),
                        row.nutritionLoggedDays(), row.stepsObservedDays(),
                        row.sleepObservedDays(), row.trainingDays(), row.averageWeightKg(),
                        row.weightTrendPercent(), row.nutritionAdherencePercent(),
                        row.averageSteps(), row.averageSleepMinutes(), row.totalTrainingMinutes(),
                        row.recommendation(), row.proposedEnergyDeltaKcal(), row.reason(),
                        row.createdAt())).toList(),
                conversations.stream().map(row -> new AccountDataExport.Conversation(
                        row.id(), row.createdAt(), row.updatedAt(),
                        messagesByConversation.getOrDefault(row.id(), List.of()))).toList()
        );
    }

    private AccountDataExport.Profile profile(AccountDataLifecycleMapper.ProfileRow row) {
        return new AccountDataExport.Profile(row.dateOfBirth(), row.calculationSex(),
                row.heightCm(), row.currentWeightKg(), row.targetWeightKg(), row.activityLevel(),
                row.timeZone(), row.screeningVersion(), row.createdAt(), row.updatedAt());
    }

    private AccountDataExport.Screening screening(AccountDataLifecycleMapper.ScreeningRow row) {
        return new AccountDataExport.Screening(row.id(), row.version(),
                row.pregnantOrBreastfeeding(), row.eatingDisorderHistory(),
                row.medicalGuidanceRequired(), row.weightAffectingMedication(),
                row.concerningSymptoms(), row.status(), row.automaticPlanningAllowed(),
                row.reasonCodes(), row.createdAt());
    }

    private AccountDataExport.PlanVersion planVersion(
            AccountDataLifecycleMapper.PlanVersionRow row) {
        return new AccountDataExport.PlanVersion(row.id(), row.versionNo(), row.status(),
                row.goal(), row.profileUpdatedAt(), row.screeningId(), row.screeningVersion(),
                row.assessmentAttemptId(), row.formulaVersion(), row.targetPolicyVersion(),
                row.bmi(), row.bmrKcalPerDay(), row.tdeeKcalPerDay(),
                row.energyMinKcalPerDay(), row.energyMaxKcalPerDay(),
                row.weeklyWeightChangeMinPercent(), row.weeklyWeightChangeMaxPercent(),
                row.createdAt(), row.validatedAt(), row.confirmedAt(), row.activatedAt(),
                row.replacedAt());
    }

    private <T> List<T> bounded(List<T> rows) {
        if (rows.size() > MAX_ROWS) throw new DataExportTooLargeException();
        return rows;
    }

    private <T> List<T> boundedNested(List<T> rows) {
        if (rows.size() > MAX_ROWS * 10) throw new DataExportTooLargeException();
        return rows;
    }
}

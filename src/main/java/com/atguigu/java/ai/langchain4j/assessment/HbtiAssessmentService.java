package com.atguigu.java.ai.langchain4j.assessment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class HbtiAssessmentService {

    private static final int MAX_IDEMPOTENCY_KEY_BYTES = 128;
    private static final int MAX_HISTORY_PAGE = 1_000_000;

    private final HbtiAssessmentMapper mapper;
    private final HbtiDefinitionCatalog definitions;
    private final HbtiScoringEngine scoringEngine;
    private final Clock clock;

    public HbtiAssessmentService(
            HbtiAssessmentMapper mapper,
            HbtiDefinitionCatalog definitions,
            HbtiScoringEngine scoringEngine,
            Clock clock
    ) {
        this.mapper = mapper;
        this.definitions = definitions;
        this.scoringEngine = scoringEngine;
        this.clock = clock;
    }

    @Transactional
    public HbtiAssessmentSubmission submit(
            String userId,
            String idempotencyKey,
            SubmitHbtiAssessmentCommand command
    ) {
        validateRequest(userId, idempotencyKey, command);
        HbtiDefinition definition = definitions.findPublished("hbti", command.definitionVersion())
                .orElseThrow(AssessmentDefinitionNotFoundException::new);
        HbtiScoreResult score = scoringEngine.score(definition, command.answers());
        String payloadHash = payloadHash(definition.version(), command.answers());
        String idempotencyKeyHash = sha256(idempotencyKey);

        if (mapper.lockUser(userId) == null) {
            throw new InvalidAssessmentRequestException("Authenticated user does not exist");
        }
        Optional<HbtiAssessmentResultRow> existing = mapper.findByIdempotencyKey(
                userId, idempotencyKeyHash
        );
        if (existing.isPresent()) {
            if (!existing.get().payloadHash().equals(payloadHash)) {
                throw new IdempotencyConflictException();
            }
            return new HbtiAssessmentSubmission(hydrate(existing.get()), true);
        }

        String attemptId = UUID.randomUUID().toString();
        Instant completedAt = clock.instant().truncatedTo(ChronoUnit.MICROS);
        mapper.insertAttempt(
                attemptId, userId, definition.id(), idempotencyKeyHash,
                payloadHash, score.typeCode(), completedAt
        );

        Map<String, HbtiItemDefinition> items = new HashMap<>();
        definition.items().forEach(item -> items.put(item.itemKey(), item));
        for (HbtiAnswer answer : command.answers()) {
            HbtiItemDefinition item = items.get(answer.itemKey());
            mapper.insertAnswer(attemptId, item.id(), answer.itemKey(), answer.value());
        }
        for (int index = 0; index < score.dimensions().size(); index++) {
            mapper.insertScore(attemptId, index + 1, score.dimensions().get(index));
        }

        return new HbtiAssessmentSubmission(new HbtiAssessmentResult(
                attemptId, score.definitionVersion(), score.scoringRuleVersion(),
                score.typeCode(), score.dimensions(), completedAt
        ), false);
    }

    @Transactional(readOnly = true)
    public Optional<HbtiAssessmentResult> current(String userId) {
        return mapper.findCurrent(userId).map(this::hydrate);
    }

    @Transactional(readOnly = true)
    public HbtiAssessmentPage history(String userId, int page, int pageSize) {
        if (page < 0 || page > MAX_HISTORY_PAGE || pageSize < 1 || pageSize > 100) {
            throw new InvalidAssessmentRequestException("Invalid history page");
        }
        List<HbtiAssessmentResult> items = mapper.findHistory(
                userId, pageSize, Math.multiplyExact(page, pageSize)
        ).stream().map(this::hydrate).toList();
        return new HbtiAssessmentPage(items, page, pageSize, mapper.countHistory(userId));
    }

    private HbtiAssessmentResult hydrate(HbtiAssessmentResultRow row) {
        return new HbtiAssessmentResult(
                row.id(), row.definitionVersion(), row.scoringRuleVersion(),
                row.typeCode(), mapper.findScores(row.id()), row.completedAt()
        );
    }

    private void validateRequest(
            String userId,
            String idempotencyKey,
            SubmitHbtiAssessmentCommand command
    ) {
        if (userId == null || userId.isBlank() || command == null
                || command.definitionVersion() == null || command.definitionVersion().isBlank()) {
            throw new InvalidAssessmentRequestException("Assessment request is incomplete");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.getBytes(StandardCharsets.UTF_8).length > MAX_IDEMPOTENCY_KEY_BYTES) {
            throw new InvalidAssessmentRequestException("Idempotency key is invalid");
        }
    }

    private String payloadHash(String definitionVersion, List<HbtiAnswer> answers) {
        StringBuilder canonical = new StringBuilder(definitionVersion).append('\n');
        answers.stream()
                .sorted(Comparator.comparing(HbtiAnswer::itemKey))
                .forEach(answer -> canonical.append(answer.itemKey())
                        .append('=').append(answer.value()).append('\n'));
        return sha256(canonical.toString());
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}

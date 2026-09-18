package com.atguigu.java.ai.langchain4j.identity;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final int MAXIMUM_ENCODED_TOKEN_LENGTH = 128;

    private final RefreshTokenMapper refreshTokenMapper;
    private final AuthProperties authProperties;
    private final SecureRandom secureRandom;
    private final Clock clock;

    public RefreshTokenService(
            RefreshTokenMapper refreshTokenMapper,
            AuthProperties authProperties,
            SecureRandom secureRandom,
            Clock clock
    ) {
        this.refreshTokenMapper = refreshTokenMapper;
        this.authProperties = authProperties;
        this.secureRandom = secureRandom;
        this.clock = clock;
    }

    @Transactional
    public IssuedRefreshToken issue(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        return create(userId, UUID.randomUUID().toString(), clock.instant());
    }

    @Transactional(noRollbackFor = RefreshTokenReuseException.class)
    public IssuedRefreshToken rotate(String rawToken) {
        validateRawToken(rawToken);
        Instant now = clock.instant();
        RefreshToken current = refreshTokenMapper.findByHashForUpdate(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (current.replacedByTokenId() != null) {
            refreshTokenMapper.revokeFamily(current.userId(), current.familyId(), now);
            throw new RefreshTokenReuseException();
        }
        if (current.revokedAt() != null || !current.expiresAt().isAfter(now)) {
            throw new InvalidRefreshTokenException();
        }

        IssuedRefreshToken replacement = create(current.userId(), current.familyId(), now);
        if (refreshTokenMapper.markRotated(current.id(), replacement.id(), now) != 1) {
            throw new InvalidRefreshTokenException();
        }
        return replacement;
    }

    @Transactional
    public void revokeFamily(String rawToken) {
        validateRawToken(rawToken);
        RefreshToken current = refreshTokenMapper.findByHashForUpdate(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);
        refreshTokenMapper.revokeFamily(current.userId(), current.familyId(), clock.instant());
    }

    private IssuedRefreshToken create(String userId, String familyId, Instant now) {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String id = UUID.randomUUID().toString();
        Instant expiresAt = now.plus(authProperties.refreshTokenTtl());
        refreshTokenMapper.insert(new RefreshToken(
                id, userId, hash(value), familyId, null, expiresAt, null, now
        ));
        return new IssuedRefreshToken(id, userId, value, expiresAt);
    }

    private static void validateRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > MAXIMUM_ENCODED_TOKEN_LENGTH) {
            throw new InvalidRefreshTokenException();
        }
    }

    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.US_ASCII));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}

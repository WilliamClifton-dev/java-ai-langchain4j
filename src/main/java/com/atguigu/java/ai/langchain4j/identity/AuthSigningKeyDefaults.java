package com.atguigu.java.ai.langchain4j.identity;

import java.util.Set;

/**
 * Constants for the auth signing-key fail-fast validator.
 *
 * <p>The well-known development key is the value the Compose stack falls back
 * to when no {@code AUTH_SIGNING_KEY} is injected. Deployments that start with
 * the same value under a production-grade profile are the failure mode this
 * ADR-016 guard exists to prevent.
 */
public final class AuthSigningKeyDefaults {

    public static final String DEFAULT_DEVELOPMENT_KEY =
            "local-development-signing-key-min-32-bytes";

    /**
     * Profiles that may legally boot with the well-known development key.
     * Mirrors the profile matrix that runs without operator-supplied secrets.
     */
    public static final Set<String> ALLOWED_DEVELOPMENT_PROFILES =
            Set.of("offline", "local", "test");

    private AuthSigningKeyDefaults() {
    }
}

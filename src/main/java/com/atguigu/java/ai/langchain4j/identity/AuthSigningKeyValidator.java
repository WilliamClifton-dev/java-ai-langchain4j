package com.atguigu.java.ai.langchain4j.identity;

import java.util.Arrays;

/**
 * Stateless check that decides whether the configured auth signing key is
 * allowed under the active Spring profiles. See ADR-016.
 *
 * <p>The well-known development key shipped with the Compose fallback is only
 * tolerated under the profiles listed in
 * {@link AuthSigningKeyDefaults#ALLOWED_DEVELOPMENT_PROFILES}. Any other key
 * passes unconditionally so platform-injected secrets remain unaffected.
 */
public final class AuthSigningKeyValidator {

    private AuthSigningKeyValidator() {
    }

    /**
     * Validate the supplied signing key against the active profile list.
     *
     * @param signingKey    the value of {@code hbti.auth.signing-key}
     * @param activeProfiles the active Spring profiles; may be empty
     * @throws IllegalStateException if the dev key is used outside the allowed
     *                               development profiles
     */
    public static void validate(String signingKey, String... activeProfiles) {
        if (signingKey == null
                || !AuthSigningKeyDefaults.DEFAULT_DEVELOPMENT_KEY.equals(signingKey)) {
            return;
        }
        boolean allowed = Arrays.stream(activeProfiles == null
                        ? new String[0] : activeProfiles)
                .anyMatch(AuthSigningKeyDefaults.ALLOWED_DEVELOPMENT_PROFILES::contains);
        if (allowed) {
            return;
        }
        throw new IllegalStateException(
                "hbti.auth.signing-key uses the well-known development value '"
                        + AuthSigningKeyDefaults.DEFAULT_DEVELOPMENT_KEY
                        + "'. Set AUTH_SIGNING_KEY to a unique 32+ byte secret. "
                        + "The default key is allowed only under profiles: "
                        + AuthSigningKeyDefaults.ALLOWED_DEVELOPMENT_PROFILES
                        + ". Active profiles: "
                        + Arrays.toString(activeProfiles));
    }
}

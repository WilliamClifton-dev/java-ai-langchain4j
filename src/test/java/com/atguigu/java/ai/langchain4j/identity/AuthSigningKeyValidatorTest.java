package com.atguigu.java.ai.langchain4j.identity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthSigningKeyValidatorTest {

    private static final String STRONG_KEY = "operator-supplied-secret-with-more-than-32-bytes";

    @Test
    void rejectsDefaultKeyWhenNoActiveProfileIsDevelopment() {
        assertThatThrownBy(() ->
                AuthSigningKeyValidator.validate(
                        AuthSigningKeyDefaults.DEFAULT_DEVELOPMENT_KEY, "minimax"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(AuthSigningKeyDefaults.DEFAULT_DEVELOPMENT_KEY)
                .hasMessageContaining("minimax")
                .hasMessageContaining("offline")
                .hasMessageContaining("local")
                .hasMessageContaining("test");
    }

    @Test
    void rejectsDefaultKeyUnderProdProfile() {
        assertThatThrownBy(() ->
                AuthSigningKeyValidator.validate(
                        AuthSigningKeyDefaults.DEFAULT_DEVELOPMENT_KEY, "prod"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsDefaultKeyWhenNoProfileIsActive() {
        assertThatThrownBy(() ->
                AuthSigningKeyValidator.validate(
                        AuthSigningKeyDefaults.DEFAULT_DEVELOPMENT_KEY))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void allowsDefaultKeyUnderOfflineProfile() {
        assertThatCode(() ->
                AuthSigningKeyValidator.validate(
                        AuthSigningKeyDefaults.DEFAULT_DEVELOPMENT_KEY, "offline"))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsDefaultKeyUnderLocalProfile() {
        assertThatCode(() ->
                AuthSigningKeyValidator.validate(
                        AuthSigningKeyDefaults.DEFAULT_DEVELOPMENT_KEY, "local"))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsDefaultKeyUnderTestProfile() {
        assertThatCode(() ->
                AuthSigningKeyValidator.validate(
                        AuthSigningKeyDefaults.DEFAULT_DEVELOPMENT_KEY, "test"))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsDefaultKeyWhenAnyActiveProfileIsDevelopment() {
        assertThatCode(() ->
                AuthSigningKeyValidator.validate(
                        AuthSigningKeyDefaults.DEFAULT_DEVELOPMENT_KEY,
                        "minimax", "offline"))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsCustomKeyRegardlessOfProfile() {
        assertThatCode(() ->
                AuthSigningKeyValidator.validate(STRONG_KEY, "prod"))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsCustomKeyWhenNoProfileIsActive() {
        assertThatCode(() ->
                AuthSigningKeyValidator.validate(STRONG_KEY))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsNullKeyAsUnset() {
        assertThatCode(() ->
                AuthSigningKeyValidator.validate(null, "minimax"))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsNullActiveProfileArrayForDefaultKey() {
        assertThatThrownBy(() ->
                AuthSigningKeyValidator.validate(
                        AuthSigningKeyDefaults.DEFAULT_DEVELOPMENT_KEY, (String[]) null))
                .isInstanceOf(IllegalStateException.class);
    }
}

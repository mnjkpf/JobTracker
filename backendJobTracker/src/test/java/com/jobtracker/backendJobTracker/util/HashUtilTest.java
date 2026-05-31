package com.jobtracker.backendJobTracker.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit-тести для {@link HashUtil} — чистий SHA-256 без Spring контексту.
 * Перевіряємо на відомих векторах (NIST/RFC) + constant-time verify.
 */
class HashUtilTest {

    @ParameterizedTest(name = "sha256(\"{0}\") = {1}")
    @CsvSource({
            // Відомі SHA-256 вектори (lower-case hex, без розділювачів).
            "abc,ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            "'',e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            "The quick brown fox jumps over the lazy dog,d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592"
    })
    @DisplayName("sha256 співпадає з відомими векторами")
    void sha256_knownVectors(String input, String expected) {
        assertThat(HashUtil.sha256(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("sha256 завжди повертає 64 hex-символи і детермінований")
    void sha256_lengthAndDeterminism() {
        String once = HashUtil.sha256("some-refresh-token");
        String twice = HashUtil.sha256("some-refresh-token");

        assertThat(once).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(once).isEqualTo(twice);
        assertThat(HashUtil.sha256("different")).isNotEqualTo(once);
    }

    @Test
    @DisplayName("sha256(null) кидає IllegalArgumentException")
    void sha256_nullThrows() {
        assertThatThrownBy(() -> HashUtil.sha256(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    @Test
    @DisplayName("verifyHash повертає true коли input хешується у expected")
    void verifyHash_match() {
        String raw = "raw-refresh-token-value";
        String hash = HashUtil.sha256(raw);

        assertThat(HashUtil.verifyHash(raw, hash)).isTrue();
    }

    @Test
    @DisplayName("verifyHash повертає false при розбіжності")
    void verifyHash_mismatch() {
        String hash = HashUtil.sha256("token-a");

        assertThat(HashUtil.verifyHash("token-b", hash)).isFalse();
        // Однакова довжина hex, але інший вміст — перевіряє побайтове порівняння.
        assertThat(HashUtil.verifyHash("token-a", HashUtil.sha256("token-c"))).isFalse();
    }

    @Test
    @DisplayName("verifyHash з null аргументами кидає IllegalArgumentException")
    void verifyHash_nullThrows() {
        assertThatThrownBy(() -> HashUtil.verifyHash(null, "x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HashUtil.verifyHash("x", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

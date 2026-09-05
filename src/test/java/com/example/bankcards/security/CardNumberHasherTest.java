package com.example.bankcards.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CardNumberHasherTest {

    @Test
    void sameNumber_shouldProduceSameHash() {
        CardNumberHasher hasher = new CardNumberHasher("test-secret-key");
        String number = "1234567890123456";

        assertThat(hasher.hash(number)).isEqualTo(hasher.hash(number));
    }

    @Test
    void differentNumbers_shouldProduceDifferentHashes() {
        CardNumberHasher hasher = new CardNumberHasher("test-secret-key");

        assertThat(hasher.hash("1111111111111111"))
                .isNotEqualTo(hasher.hash("2222222222222222"));
    }

    @Test
    void differentSecrets_shouldProduceDifferentHashes_provingItIsKeyed() {
        String number = "1234567890123456";
        CardNumberHasher hasherA = new CardNumberHasher("secret-a");
        CardNumberHasher hasherB = new CardNumberHasher("secret-b");

        assertThat(hasherA.hash(number)).isNotEqualTo(hasherB.hash(number));
    }

    @Test
    void hash_shouldNotBePlainSha256OfNumber() throws Exception {
        String number = "1234567890123456";
        CardNumberHasher hasher = new CardNumberHasher("test-secret-key");

        java.security.MessageDigest sha256 = java.security.MessageDigest.getInstance("SHA-256");
        String plainSha256 = java.util.Base64.getEncoder().encodeToString(
                sha256.digest(number.getBytes(java.nio.charset.StandardCharsets.UTF_8))
        );

        assertThat(hasher.hash(number)).isNotEqualTo(plainSha256);
    }
}

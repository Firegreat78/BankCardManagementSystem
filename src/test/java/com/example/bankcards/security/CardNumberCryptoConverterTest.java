package com.example.bankcards.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CardNumberCryptoConverterTest {

    private final CardNumberCryptoConverter converter =
            new CardNumberCryptoConverter("test-secret-key");

    @Test
    void sameNumber_encryptedTwice_shouldProduceDifferentCiphertext() {
        String plain = "1234567890123456";

        String cipherText1 = converter.convertToDatabaseColumn(plain);
        String cipherText2 = converter.convertToDatabaseColumn(plain);

        assertThat(cipherText1).isNotEqualTo(cipherText2);
    }

    @Test
    void encryptThenDecrypt_shouldReturnOriginalNumber() {
        String plain = "1234567890123456";

        String cipherText = converter.convertToDatabaseColumn(plain);

        assertThat(converter.convertToEntityAttribute(cipherText)).isEqualTo(plain);
    }

    @Test
    void bothEncryptionsOfSameNumber_shouldDecryptToSameValue() {
        String plain = "1234567890123456";

        String cipherText1 = converter.convertToDatabaseColumn(plain);
        String cipherText2 = converter.convertToDatabaseColumn(plain);

        assertThat(converter.convertToEntityAttribute(cipherText1)).isEqualTo(plain);
        assertThat(converter.convertToEntityAttribute(cipherText2)).isEqualTo(plain);
    }
}

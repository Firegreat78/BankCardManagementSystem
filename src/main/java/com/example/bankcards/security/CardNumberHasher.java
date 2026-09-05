package com.example.bankcards.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Produces a deterministic HMAC-SHA256 lookup value for a card number, used
 * to find/detect duplicate numbers without comparing the randomized
 * AES-GCM ciphertext stored in {@code Card.number}.
 */
@Component
public class CardNumberHasher {

    private static final String MAC_ALGO = "HmacSHA256";

    private final SecretKeySpec key;

    public CardNumberHasher(@Value("${card.encryption.secret}") String secret) {
        this.key = new SecretKeySpec(
                (secret + ":lookup").getBytes(StandardCharsets.UTF_8),
                MAC_ALGO
        );
    }

    public String hash(String plainNumber) {
        try {
            Mac mac = Mac.getInstance(MAC_ALGO);
            mac.init(key);
            byte[] digest = mac.doFinal(plainNumber.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash card number", e);
        }
    }
}

package com.example.bankcards.controller;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class JwtTest {
    @Test
    void tokenGenerationAndValidation_shouldWork() {
        String token = jwtUtil.generateToken("user1");
        assertThat(token).isNotBlank();

        String username = jwtUtil.extractUsername(token);
        assertThat(username).isEqualTo("user1");

        boolean valid = jwtUtil.validateToken(token, "user1");
        assertThat(valid).isTrue();
    }
}
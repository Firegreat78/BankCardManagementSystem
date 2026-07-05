package com.example.bankcards.controller;

import com.example.bankcards.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@SuppressWarnings("unused")
class JwtTest {
    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void tokenGenerationAndValidation_shouldWork() {
        String initialUsername = "user1";
        String token = jwtUtil.generateToken(initialUsername);
        assertThat(token).isNotBlank();

        String extractedUsername = jwtUtil.extractUsername(token);
        assertThat(extractedUsername).isEqualTo(initialUsername);

        boolean valid = jwtUtil.validateToken(token, initialUsername);
        assertThat(valid).isTrue();
    }
}
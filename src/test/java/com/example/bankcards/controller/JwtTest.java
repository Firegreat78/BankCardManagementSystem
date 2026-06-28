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
        String token = jwtUtil.generateToken("user1");
        assertThat(token).isNotBlank();

        String username = jwtUtil.extractUsername(token);
        assertThat(username).isEqualTo("user1");

        boolean valid = jwtUtil.validateToken(token, "user1");
        assertThat(valid).isTrue();
    }
}
package com.example.bankcards.controller;

import com.example.bankcards.security.JwtUtil;
import com.example.bankcards.entity.Role;
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
        Role initialRole = Role.USER;
        String token = jwtUtil.generateToken(initialUsername, initialRole);

        assertThat(token).isNotBlank();

        String extractedUsername = jwtUtil.extractUsername(token);
        Role extractedRole = jwtUtil.extractRole(token);

        assertThat(extractedUsername).isEqualTo(initialUsername);
        assertThat(extractedRole).isEqualTo(initialRole);

        boolean valid = jwtUtil.validateToken(token, initialUsername, initialRole);
        assertThat(valid).isTrue();
    }
}
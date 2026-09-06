package com.example.bankcards.controller;

import com.example.bankcards.entity.Role;
import com.example.bankcards.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A token that cannot be trusted must leave the request unauthenticated (401),
 * never let the parsing failure escape the filter chain as a 500.
 */
class JwtAuthenticationTest extends com.example.bankcards.IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Test
    void expiredToken_shouldReturn401() throws Exception {
        JwtUtil expiredTokenFactory = new JwtUtil();
        ReflectionTestUtils.setField(expiredTokenFactory, "secret", jwtSecret);
        ReflectionTestUtils.setField(expiredTokenFactory, "expirationMs", -60_000L);
        String expiredToken = expiredTokenFactory.generateToken("alice", Role.USER);

        mockMvc.perform(get("/cards").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/cards").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedWithAnotherKey_shouldReturn401() throws Exception {
        JwtUtil foreignTokenFactory = new JwtUtil();
        ReflectionTestUtils.setField(
                foreignTokenFactory,
                "secret",
                "a-completely-different-signing-key-0123456789abcdef"
        );
        ReflectionTestUtils.setField(foreignTokenFactory, "expirationMs", 60_000L);
        String foreignToken = foreignTokenFactory.generateToken("alice", Role.ADMIN);

        mockMvc.perform(get("/cards").header("Authorization", "Bearer " + foreignToken))
                .andExpect(status().isUnauthorized());
    }
}

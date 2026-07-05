package com.example.bankcards.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("unused")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserControllerAuthTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        String correctAdminUsername = "user";
        String correctAdminPassword = "pass";

        Map<String, Object> adminMap = Map.of(
                "username", correctAdminUsername,
                "password", correctAdminPassword
        );
        objectMapper.writeValueAsString(adminMap);
        String adminLoginJson = objectMapper.writeValueAsString(adminMap);
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminLoginJson))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        adminToken = objectMapper.readTree(json).get("token").asText();
    }

    @Test
    void registerUser_shouldReturnCreatedUser() throws Exception {
        Map<String, Object> userMap = Map.of(
                "username", "alice",
                "password", "alice123"
        );
        String userJson = objectMapper.writeValueAsString(userMap);

        mockMvc.perform(post("/users/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("alice"));
    }
}
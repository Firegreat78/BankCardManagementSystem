package com.example.bankcards.controller;

import com.example.bankcards.config.AdminConfig;
import com.example.bankcards.util.Utility;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SuppressWarnings("unused")
class AuthControllerTest {
    @Autowired
    AdminConfig adminConfig;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    Utility utility;

    @Test
    void login_validCredentials_returnsToken() throws Exception {
        Map<String, Object> adminMap = Map.of(
                "username", adminConfig.getUsername(), "password", adminConfig.getPassword()
        );
        String loginJson = objectMapper.writeValueAsString(adminMap);
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        Map<String, Object> adminMap = Map.of(
                "username", adminConfig.getUsername(), "password", adminConfig.getPassword() + "0"
        );
        String loginJson = objectMapper.writeValueAsString(adminMap);
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unregisteredUser_shouldReturn401() throws Exception {
        Map<String, Object> loginMap = Map.of(
                "username", "unknown",
                "password", "unknown123"
        );
        String loginJson = objectMapper.writeValueAsString(loginMap);
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_registeredUser_shouldReturn200() throws Exception {
        String username = "testuser";
        String password = "testpass123";
        String adminToken = utility.loginAdmin(mockMvc);
        System.out.println("adminToken: " + adminToken);
        Map<String, Object> registerMap = Map.of("username", username, "password", password);
        String registerJson = objectMapper.writeValueAsString(registerMap);
        mockMvc.perform(post("/users/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andDo(print())
                .andExpect(status().isCreated());
        Map<String, Object> loginMap = Map.of("username", username, "password", password);

        String loginJson = objectMapper.writeValueAsString(loginMap);
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());
    }
}
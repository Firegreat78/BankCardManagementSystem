package com.example.bankcards.controller;

import com.example.bankcards.config.AdminConfig;
import com.example.bankcards.util.Utility;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("unused")
class AuthControllerTest extends com.example.bankcards.IntegrationTest {
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
        utility.loginUserAction(mockMvc, adminConfig.getUsername(), adminConfig.getPassword())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        String invalidUsername = adminConfig.getUsername() + "0";
        utility.loginUserAction(mockMvc, invalidUsername, "pass")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerUser_withAdminToken_shouldReturnCreatedUser() throws Exception {
        String adminToken = utility.loginAdmin(mockMvc);

        utility.registerUserAction(mockMvc, adminToken, "alice", "pass")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void registerUser_shouldNotReturnPassword() throws Exception {
        String adminToken = utility.loginAdmin(mockMvc);

        utility.registerUserAction(mockMvc, adminToken, "alice", "pass")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registerUser_withUserToken_isForbidden() throws Exception {
        String adminToken = utility.loginAdmin(mockMvc);

        utility.registerUser(mockMvc, adminToken, "user1", "pass1");
        String userToken = utility.loginUser(mockMvc, "user1", "pass1");

        utility.registerUserAction(mockMvc, userToken, "user2", "pass2")
                .andExpect(status().isForbidden());
    }

    @Test
    void login_registeredUser_shouldReturn200() throws Exception {
        String adminToken = utility.loginAdmin(mockMvc);
        String id = utility.registerUser(mockMvc, adminToken, "user", "pass");
        utility.loginUserAction(mockMvc, "user", "pass")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());

    }
}
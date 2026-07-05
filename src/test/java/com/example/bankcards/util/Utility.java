package com.example.bankcards.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class Utility
{
    public static String mockRegisterUser(
            String adminToken,
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            String username,
            String password) throws Exception {
        Map<String, Object> userMap = Map.of(
                "username", username,
                "password", password
        );
        String userJson = objectMapper.writeValueAsString(userMap);
        MvcResult result = mockMvc.perform(post("/users/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asText();
    }

    public static String getUserToken(
            String adminToken,
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            String username,
            String password) throws Exception {
        Map<String, Object> userMap = Map.of(
                "username", username,
                "password", password
        );
        String userJson = objectMapper.writeValueAsString(userMap);
        mockMvc.perform(post("/users/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated());

        Map<String, Object> loginMap = Map.of(
                "username", username,
                "password", password
        );
        String loginJson = objectMapper.writeValueAsString(loginMap);
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("token").asText();
    }
}

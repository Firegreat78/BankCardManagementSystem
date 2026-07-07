package com.example.bankcards.util;

import com.example.bankcards.config.AdminConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Component
@RequiredArgsConstructor
public class Utility {

    private final AdminConfig adminConfig;
    private final ObjectMapper objectMapper;

    public String cardNum(int n) {
        return String.format("%016d", n);
    }

    public String mockRegisterUser(
            String adminToken,
            MockMvc mockMvc,
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

    public String loginAdmin(MockMvc mockMvc) throws Exception {
        Map<String, Object> loginMap = Map.of(
                "username", adminConfig.getUsername(),
                "password", adminConfig.getPassword()
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

    public String getUserToken(
            MockMvc mockMvc,
            String username,
            String password) throws Exception {

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
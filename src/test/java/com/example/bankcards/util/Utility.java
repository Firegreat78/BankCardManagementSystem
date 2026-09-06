package com.example.bankcards.util;

import com.example.bankcards.config.AdminConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Component
@RequiredArgsConstructor
public class Utility {
    @Autowired
    private AdminConfig adminConfig;

    @Autowired
    private ObjectMapper objectMapper;

    public String generateCardNum(int n) throws ArithmeticException {
        if (n < 0) throw new ArithmeticException("card number should not be negative");
        return String.format("%016d", n);
    }

    public ResultActions registerUserAction(
            MockMvc mockMvc,
            String token,
            String username,
            String password) throws Exception {
        return mockMvc.perform(post("/users/register")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "username", username,
                        "password", password
                ))));
    }

    public ResultActions registerUserAction(
            MockMvc mockMvc,
            String token,
            String username,
            String password,
            String role) throws Exception {
        return mockMvc.perform(post("/users/register")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "username", username,
                        "password", password,
                        "role", role
                ))));
    }

    public String registerUser(
            MockMvc mockMvc,
            String adminToken,
            String username,
            String password) throws Exception {
        MvcResult result = registerUserAction(mockMvc, adminToken, username, password)
                .andExpect(status().isCreated())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asText();
    }

    public ResultActions loginUserAction(
            MockMvc mockMvc,
            String username,
            String password) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "username", username,
                        "password", password
                ))));
    }

    public String loginUser(
            MockMvc mockMvc,
            String username,
            String password) throws Exception {
        MvcResult result = loginUserAction(mockMvc, username, password)
                .andExpect(status().isOk())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("token").asText();
    }

    public String loginAdmin(MockMvc mockMvc) throws Exception {
        return loginUser(mockMvc, adminConfig.getUsername(), adminConfig.getPassword());
    }

    public ResultActions getCardAction(MockMvc mockMvc, String token) throws Exception {
        return mockMvc.perform(get("/cards").header("Authorization", "Bearer " + token));
    }

    public ResultActions getCardAction(MockMvc mockMvc, String token, String cardId) throws Exception {
        return mockMvc.perform(get("/cards/" + cardId)
                .header("Authorization", "Bearer " + token));
    }

    public ResultActions getCardAction(MockMvc mockMvc, String token, String cardId, Integer page, Integer size) throws Exception {
        return mockMvc.perform(get("/cards/" + cardId)
                .param("page", String.valueOf(page))
                .header("Authorization", "Bearer " + token));
    }

    public ResultActions createCardAction(
            MockMvc mockMvc,
            String token,
            String number,
            String holderId,
            BigDecimal balance
    ) throws Exception {
        return createCardAction(
                mockMvc,
                token,
                number,
                holderId,
                balance,
                LocalDate.now().plusYears(3)
        );
    }

    public ResultActions createCardAction(
            MockMvc mockMvc,
            String token,
            int number,
            String holderId,
            BigDecimal balance
    ) throws Exception {
        return createCardAction(mockMvc, token, generateCardNum(number), holderId, balance);
    }

    public ResultActions createCardAction(
            MockMvc mockMvc,
            String token,
            String number,
            String holderId,
            BigDecimal balance,
            LocalDate expirationDate
    ) throws Exception {
        return mockMvc.perform(post("/cards")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "number", number,
                        "holderId", holderId,
                        "balance", balance,
                        "expirationDate", expirationDate
                ))));
    }

    public ResultActions createCardAction(
            MockMvc mockMvc,
            String token,
            int number,
            String holderId,
            BigDecimal balance,
            LocalDate expirationDate
    ) throws Exception {
        return createCardAction(mockMvc, token, generateCardNum(number), holderId, balance, expirationDate);
    }

    public String createCard(
            MockMvc mockMvc,
            String adminToken,
            String number,
            String holderId,
            BigDecimal balance) throws Exception {
        MvcResult result = createCardAction(mockMvc, adminToken, number, holderId, balance)
                .andExpect(status().isOk())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asText();
    }

    public String createCard(
            MockMvc mockMvc,
            String adminToken,
            int number,
            String holderId,
            BigDecimal balance) throws Exception {
        return createCard(mockMvc, adminToken, generateCardNum(number), holderId, balance);
    }

    public ResultActions deleteCardAction(MockMvc mockMvc, String token, String cardId) throws Exception {
        return mockMvc.perform(delete("/cards/" + cardId)
                .header("Authorization", "Bearer " + token));
    }

    public ResultActions createTransferAction(
            MockMvc mockMvc,
            String token,
            String fromCardId,
            String toCardId,
            BigDecimal balance) throws Exception {
        return mockMvc.perform(post("/cards/transfer")
                .param("fromId", fromCardId)
                .param("toId", toCardId)
                .param("amount", balance.toString())
                .header("Authorization", "Bearer " + token));
    }

    public ResultActions requestCardBlockAction(MockMvc mockMvc, String userToken, String cardId) throws Exception {
        return mockMvc.perform(patch("/cards/" + cardId + "/block-request")
                .header("Authorization", "Bearer " + userToken));
    }

    public ResultActions requestCardUnblockAction(MockMvc mockMvc, String userToken, String cardId) throws Exception {
        return mockMvc.perform(patch("/cards/" + cardId + "/unblock-request")
                .header("Authorization", "Bearer " + userToken));
    }

    public ResultActions blockCardAction(MockMvc mockMvc, String adminToken, String cardId) throws Exception {
        return mockMvc.perform(patch("/cards/" + cardId + "/block")
                .header("Authorization", "Bearer " + adminToken));
    }

    public ResultActions activateCardAction(MockMvc mockMvc, String adminToken, String cardId) throws Exception {
        return mockMvc.perform(patch("/cards/" + cardId + "/activate")
                .header("Authorization", "Bearer " + adminToken));
    }
}
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

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("unused")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    private String userId1;
    private String userId2;

    private String registerUser(String username, String password) throws Exception {
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

    @BeforeEach
    void setUp() throws Exception {
        String correctAdminUsername = "user";
        String correctAdminPassword = "pass";
        Map<String, Object> adminMap = Map.of(
                "username", correctAdminUsername,
                "password", correctAdminPassword
        );
        String loginJson = objectMapper.writeValueAsString(adminMap);
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        adminToken = objectMapper.readTree(json).get("token").asText();

        // Create test users
        userId1 = registerUser("alice", "alice123");
        userId2 = registerUser("bob", "bob123");
    }

    private String createCard(String number, String holderId, BigDecimal balance) throws Exception {
        Map<String, Object> cardMap = Map.of(
                "number", number,
                "holderId", holderId,
                "balance", balance
        );
        String cardJson = objectMapper.writeValueAsString(cardMap);

        MvcResult result = mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asText();
    }

    @Test
    void createCard_shouldReturnCard() throws Exception {
        String cardNum = "4111111111111111";
        long cardBalance = 1000;

        Map<String, Object> cardMap = Map.of(
                "number", cardNum,
                "holderId", userId1,
                "balance", cardBalance
        );
        String cardJson = objectMapper.writeValueAsString(cardMap);

        mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.number").value(cardNum))
                .andExpect(jsonPath("$.holderId").value(userId1))
                .andExpect(jsonPath("$.balance").value(cardBalance));
    }

    @Test
    void createCard_negativeBalance_shouldReturn400() throws Exception {
        Map<String, Object> cardMap = Map.of(
                "number", "4222222222222222",
                "holderId", userId1,
                "balance", -100
        );
        String cardJson = objectMapper.writeValueAsString(cardMap);

        mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCard_invalidNumberLength_shouldReturn400() throws Exception {
        Map<String, Object> cardMap = Map.of(
                "number", "123",
                "holderId", userId1,
                "balance", 1000
        );
        String cardJson = objectMapper.writeValueAsString(cardMap);

        mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCard_numberWithLetters_shouldReturn400() throws Exception {
        Map<String, Object> cardMap = Map.of(
                "number", "1234abcd5678efgh",
                "holderId", userId1,
                "balance", 1000
        );
        String cardJson = objectMapper.writeValueAsString(cardMap);

        mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transfer_insufficientBalance_shouldReturn400() throws Exception {
        String fromId = createCard("4333333333333333", userId1, BigDecimal.valueOf(1000));
        String toId = createCard("4444444444444444", userId2, BigDecimal.valueOf(500));

        mockMvc.perform(post("/cards/transfer")
                        .param("fromId", fromId)
                        .param("toId", toId)
                        .param("amount", "2000")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transfer_valid_shouldSucceed() throws Exception {
        String fromId = createCard("4555555555555555", userId1, BigDecimal.valueOf(1000));
        String toId = createCard("4666666666666666", userId2, BigDecimal.valueOf(500));

        mockMvc.perform(post("/cards/transfer")
                        .param("fromId", fromId)
                        .param("toId", toId)
                        .param("amount", "200")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/cards")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + fromId + "')].balance").value(800))
                .andExpect(jsonPath("$[?(@.id=='" + toId + "')].balance").value(700));
    }

    @Test
    void deleteCard_shouldReturn200() throws Exception {
        String id = createCard("4777777777777777", userId1, BigDecimal.valueOf(1000));

        mockMvc.perform(delete("/cards/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/cards")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + id + "')]").doesNotExist());
    }

    @Test
    void deleteCard_nonExistent_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/cards/999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCard_afterDeletion_fetchShouldReturn404() throws Exception {
        // Create card
        String id = createCard("4111111111111111", userId1, BigDecimal.valueOf(1000));

        // Fetch card - should exist
        mockMvc.perform(get("/cards/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.number").value("4111111111111111"));

        // Delete card
        mockMvc.perform(delete("/cards/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Fetch again - should 404
        mockMvc.perform(get("/cards/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCard_shouldReturnUpdated() throws Exception {
        String id = createCard("4888888888888888", userId1, BigDecimal.valueOf(1000));

        Map<String, Object> updateMap = Map.of(
                "number", "4999999999999999",
                "holderId", userId1,
                "balance", 2000
        );
        String updateJson = objectMapper.writeValueAsString(updateMap);

        mockMvc.perform(put("/cards/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("4999999999999999"))
                .andExpect(jsonPath("$.holderId").value(userId1))
                .andExpect(jsonPath("$.balance").value(2000));
    }

    @Test
    void updateCard_nonExistent_shouldReturn404() throws Exception {
        Map<String, Object> updateMap = Map.of(
                "number", "4111111111111111",
                "holderId", userId1,
                "balance", 1000
        );
        String updateJson = objectMapper.writeValueAsString(updateMap);

        mockMvc.perform(put("/cards/999")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCards_shouldReturnList() throws Exception {
        createCard("4111111111111111", userId1, BigDecimal.valueOf(1000));
        createCard("4222222222222222", userId2, BigDecimal.valueOf(500));

        mockMvc.perform(get("/cards")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getCards_onlyPageWithoutSize_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/cards")
                        .param("page", "0")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCards_onlySizeWithoutPage_shouldReturnFirstPage() throws Exception {
        // Create 5 cards for userId1
        for (int i = 1; i <= 5; i++) {
            String num = String.format("%016d", i);
            createCard(num, userId1, BigDecimal.valueOf(100 * i));
        }

        mockMvc.perform(get("/cards")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].number").value("0000000000000001"));
    }

    @Test
    void getCards_withPagination_shouldReturnPaginated() throws Exception {
        // Create 5 cards for userId1
        for (int i = 1; i <= 5; i++) {
            String num = String.format("%016d", i);
            createCard(num, userId1, BigDecimal.valueOf(100 * i));
        }

        mockMvc.perform(get("/cards")
                        .param("page", "0")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/cards")
                        .param("page", "1")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/cards")
                        .param("page", "2")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getCards_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/cards"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCard_duplicateNumber_shouldReturn409() throws Exception {
        String cardNum = "4000000000000000";

        // First card
        Map<String, Object> cardMap = Map.of(
                "number", cardNum,
                "holderId", userId1,
                "balance", 1000
        );
        String cardJson = objectMapper.writeValueAsString(cardMap);

        mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson))
                .andExpect(status().isOk());

        // Duplicate card number
        mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson))
                .andExpect(status().isConflict());
    }

    @Test
    void createCard_sameHolderDifferentNumber_shouldSucceed() throws Exception {
        // First card with userId1
        Map<String, Object> cardMap1 = Map.of(
                "number", "4111111111111111",
                "holderId", userId1,
                "balance", 1000
        );
        String cardJson1 = objectMapper.writeValueAsString(cardMap1);

        mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson1))
                .andExpect(status().isOk());

        // Second card - same holderId, different number
        Map<String, Object> cardMap2 = Map.of(
                "number", "4222222222222222",
                "holderId", userId1,
                "balance", 500
        );
        String cardJson2 = objectMapper.writeValueAsString(cardMap2);

        mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("4222222222222222"))
                .andExpect(jsonPath("$.holderId").value(userId1));
    }
}
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

import static com.example.bankcards.util.Utility.*;
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

    private static final String USER1_USERNAME = "alice";
    private static final String USER1_PASSWORD = "alice123";
    private static final String USER2_USERNAME = "bob";
    private static final String USER2_PASSWORD = "bob123";

    @BeforeEach
    void setUp() throws Exception {
        adminToken = loginAdmin();
        userId1 = registerUser(USER1_USERNAME, USER1_PASSWORD);
        userId2 = registerUser(USER2_USERNAME, USER2_PASSWORD);
    }

    private String loginAdmin() throws Exception {
        Map<String, Object> loginMap = Map.of("username", ADMIN_USERNAME, "password", ADMIN_PASSWORD);
        String loginJson = objectMapper.writeValueAsString(loginMap);
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("token").asText();
    }

    private String registerUser(String username, String password) throws Exception {
        Map<String, Object> userMap = Map.of("username", username, "password", password);
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

    private String cardJson(String number, String holderId, long balance) throws Exception {
        Map<String, Object> cardMap = Map.of(
                "number", number,
                "holderId", holderId,
                "balance", balance
        );
        return objectMapper.writeValueAsString(cardMap);
    }

    @Test
    void createCard_shouldReturnCard() throws Exception {
        String num = cardNum(1);
        long balance = 1000;

        mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson(num, userId1, balance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.number").value(num))
                .andExpect(jsonPath("$.holderId").value(userId1))
                .andExpect(jsonPath("$.balance").value(balance));
    }

    @Test
    void createCard_negativeBalance_shouldReturn400() throws Exception {
        mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson(cardNum(1), userId1, -100)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCard_invalidNumberLength_shouldReturn400() throws Exception {
        String invalidNum = cardNum(1) + "1";
        mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson(invalidNum, userId1, 1000)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCard_numberWithLetters_shouldReturn400() throws Exception {
        mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("1234abcd5678efgh", userId1, 1000)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCard_duplicateNumber_shouldReturn409() throws Exception {
        String num = cardNum(1);
        String json = cardJson(num, userId1, 1000);

        mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());
    }

    @Test
    void createCard_sameHolderDifferentNumber_shouldSucceed() throws Exception {
        String num1 = cardNum(1);
        String num2 = cardNum(2);

        mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson(num1, userId1, 1000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(num1))
                .andExpect(jsonPath("$.holderId").value(userId1));

        mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson(num2, userId1, 500)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(num2))
                .andExpect(jsonPath("$.holderId").value(userId1));
    }

    @Test
    void transfer_insufficientBalance_shouldReturn400() throws Exception {
        String fromId = createCard(cardNum(1), userId1, BigDecimal.valueOf(1000));
        String toId = createCard(cardNum(2), userId2, BigDecimal.valueOf(500));

        mockMvc.perform(post("/cards/transfer")
                        .param("fromId", fromId)
                        .param("toId", toId)
                        .param("amount", "2000")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transfer_valid_shouldSucceed() throws Exception {
        String fromId = createCard(cardNum(1), userId1, BigDecimal.valueOf(1000));
        String toId = createCard(cardNum(2), userId2, BigDecimal.valueOf(500));

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
        String id = createCard(cardNum(1), userId1, BigDecimal.valueOf(1000));

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
        String num = cardNum(1);
        String id = createCard(num, userId1, BigDecimal.valueOf(1000));

        mockMvc.perform(get("/cards/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.number").value(num));

        mockMvc.perform(delete("/cards/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/cards/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCard_shouldReturnUpdated() throws Exception {
        String num = cardNum(1);
        String id = createCard(num, userId1, BigDecimal.valueOf(1000));
        long newBalance = 2000;

        Map<String, Object> updateMap = Map.of(
                "number", num,
                "holderId", userId1,
                "balance", newBalance
        );
        String updateJson = objectMapper.writeValueAsString(updateMap);

        mockMvc.perform(put("/cards/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(num))
                .andExpect(jsonPath("$.holderId").value(userId1))
                .andExpect(jsonPath("$.balance").value(newBalance));
    }

    @Test
    void updateCard_nonExistent_shouldReturn404() throws Exception {
        mockMvc.perform(put("/cards/999")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson(cardNum(1), userId1, 1000)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCards_shouldReturnList() throws Exception {
        createCard(cardNum(1), userId1, BigDecimal.valueOf(1000));
        createCard(cardNum(2), userId2, BigDecimal.valueOf(500));

        mockMvc.perform(get("/cards")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getCards_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/cards"))
                .andExpect(status().isUnauthorized());
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
        for (int i = 1; i <= 5; i++) {
            createCard(cardNum(i), userId1, BigDecimal.valueOf(100 * i));
        }

        mockMvc.perform(get("/cards")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].number").value(cardNum(1)))
                .andExpect(jsonPath("$[1].number").value(cardNum(2)));
    }

    @Test
    void getCards_withPagination_shouldReturnPaginated() throws Exception {
        for (int i = 1; i <= 5; i++) {
            createCard(cardNum(i), userId1, BigDecimal.valueOf(100 * i));
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
}
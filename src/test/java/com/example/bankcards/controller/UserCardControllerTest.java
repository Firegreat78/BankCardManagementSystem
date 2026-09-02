package com.example.bankcards.controller;

import com.example.bankcards.config.AdminConfig;
import com.example.bankcards.util.Utility;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/*
* UserCardControllerTest - тесты для действий пользователя.
Возможности юзера:
Просматривает свои карты (поиск + пагинация)
Запрашивает блокировку карты
Делает переводы между своими картами
Смотрит баланс
* */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SuppressWarnings("unused")
@ActiveProfiles("test")
class UserCardControllerTest {

    @Autowired
    private AdminConfig adminConfig;

    @Autowired
    private Utility utility;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    private String userId1 = null;
    private String userId2 = null;

    private String userToken1 = null;
    private String userToken2 = null;

    private static final String USER1_USERNAME = "alice";
    private static final String USER1_PASSWORD = "alice123";
    private static final String USER2_USERNAME = "bob";
    private static final String USER2_PASSWORD = "bob123";

    @BeforeEach
    void setUp() throws Exception {
        adminToken = utility.loginAdmin(mockMvc);

        userId1 = utility.registerUser(mockMvc, adminToken, USER1_USERNAME, USER1_PASSWORD);
        userId2 = utility.registerUser(mockMvc, adminToken, USER2_USERNAME, USER2_PASSWORD);

        userToken1 = utility.loginUser(mockMvc, USER1_USERNAME, USER1_PASSWORD);
        userToken2 = utility.loginUser(mockMvc, USER2_USERNAME, USER2_PASSWORD);
    }

    @Test
    void createCard_withoutToken_shouldReturn401() throws Exception {
        Map<String, Object> cardMap = Map.of(
                "number", utility.generateCardNum(1),
                "holderId", userId1,
                "balance", 1000
        );
        String cardJson = objectMapper.writeValueAsString(cardMap);
        mockMvc.perform(post("/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCard_withAdminToken_shouldReturn201() throws Exception {
        String cardNum = utility.generateCardNum(1);
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.number").value(cardNum));
    }

    @Test
    void createCard_withUserToken_shouldReturn403() throws Exception {
        Map<String, Object> cardMap = Map.of(
                "number", utility.generateCardNum(2),
                "holderId", userId1,
                "balance", 500
        );
        String cardJson = objectMapper.writeValueAsString(cardMap);
        mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + userToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCard_negativeBalance_shouldReturn400() throws Exception {
        Map<String, Object> cardMap = Map.of(
                "number", utility.generateCardNum(3),
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
    void blockCard_withAdminToken_shouldReturn200() throws Exception {
        Map<String, Object> cardMap = Map.of(
                "number", utility.generateCardNum(4),
                "holderId", userId1,
                "balance", 1000
        );
        String cardJson = objectMapper.writeValueAsString(cardMap);
        MvcResult result = mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        String id = objectMapper.readTree(json).get("id").asText();

        mockMvc.perform(patch("/cards/" + id + "/block")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void activateCard_withAdminToken_shouldReturn200() throws Exception {
        Map<String, Object> cardMap = Map.of(
                "number", utility.generateCardNum(5),
                "holderId", userId1,
                "balance", 1000
        );
        String cardJson = objectMapper.writeValueAsString(cardMap);
        MvcResult result = mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        String id = objectMapper.readTree(json).get("id").asText();

        mockMvc.perform(patch("/cards/" + id + "/block")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/cards/" + id + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void deleteCard_withAdminToken_shouldReturn200() throws Exception {
        Map<String, Object> cardMap = Map.of(
                "number", utility.generateCardNum(6),
                "holderId", userId1,
                "balance", 1000
        );
        String cardJson = objectMapper.writeValueAsString(cardMap);
        MvcResult result = mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        String id = objectMapper.readTree(json).get("id").asText();

        mockMvc.perform(delete("/cards/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void deleteCard_notOwnedByUser_shouldReturn403() throws Exception {
        Map<String, Object> cardMap = Map.of(
                "number", utility.generateCardNum(7),
                "holderId", userId1,
                "balance", 1000
        );
        String cardJson = objectMapper.writeValueAsString(cardMap);
        MvcResult result = mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        String id = objectMapper.readTree(json).get("id").asText();

        mockMvc.perform(delete("/cards/" + id)
                        .header("Authorization", "Bearer " + userToken2))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteCard_nonExistent_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/cards/999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllCards_withAdminToken_shouldReturnList() throws Exception {
        mockMvc.perform(get("/cards/all")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAllCards_withUserToken_shouldReturn403() throws Exception {
        mockMvc.perform(get("/cards/all")
                        .header("Authorization", "Bearer " + userToken1))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewOwnCards_withUserToken_shouldReturnOnlyOwned() throws Exception {
        Map<String, Object> cardMap = Map.of(
                "number", utility.generateCardNum(8),
                "holderId", userId1,
                "balance", 1000
        );
        String cardJson = objectMapper.writeValueAsString(cardMap);
        mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson))
                .andExpect(status().isOk());

        mockMvc.perform(get("/cards")
                        .header("Authorization", "Bearer " + userToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].ownerId").value(USER1_USERNAME));
    }

    @Test
    void viewOwnCards_withPagination_shouldReturnPaginated() throws Exception {
        for (int i = 0; i < 3; i++) {
            Map<String, Object> cardMap = Map.of(
                    "number", utility.generateCardNum(9 + i),
                    "holderId", userId1,
                    "balance", 1000
            );
            String cardJson = objectMapper.writeValueAsString(cardMap);
            mockMvc.perform(post("/cards")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cardJson))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/cards")
                        .param("page", "0")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + userToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/cards")
                        .param("page", "1")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + userToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void requestBlockCard_withUserToken_shouldReturn200() throws Exception {
        Map<String, Object> cardMap = Map.of(
                "number", utility.generateCardNum(12),
                "holderId", userId1,
                "balance", 1000
        );
        String cardJson = objectMapper.writeValueAsString(cardMap);
        MvcResult result = mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        String id = objectMapper.readTree(json).get("id").asText();

        mockMvc.perform(patch("/cards/" + id + "/block-request")
                        .header("Authorization", "Bearer " + userToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCK_REQUESTED"));
    }

    @Test
    void transferBetweenOwnCards_withUserToken_shouldReturn200() throws Exception {
        Map<String, Object> cardMap1 = Map.of(
                "number", utility.generateCardNum(13),
                "holderId", userId1,
                "balance", 1000
        );
        Map<String, Object> cardMap2 = Map.of(
                "number", utility.generateCardNum(14),
                "holderId", userId1,
                "balance", 100
        );
        String cardJson1 = objectMapper.writeValueAsString(cardMap1);
        String cardJson2 = objectMapper.writeValueAsString(cardMap2);
        MvcResult result1 = mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson1))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult result2 = mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson2))
                .andExpect(status().isOk())
                .andReturn();

        String json1 = result1.getResponse().getContentAsString();
        String json2 = result2.getResponse().getContentAsString();
        String fromId = objectMapper.readTree(json1).get("id").asText();
        String toId = objectMapper.readTree(json2).get("id").asText();

        mockMvc.perform(post("/cards/transfer")
                        .param("fromId", fromId)
                        .param("toId", toId)
                        .param("amount", "200")
                        .header("Authorization", "Bearer " + userToken1))
                .andExpect(status().isOk());
    }

    @Test
    void transferToNotOwnCard_withUserToken_shouldReturn403() throws Exception {
        Map<String, Object> cardMapAlice = Map.of(
                "number", utility.generateCardNum(15),
                "holderId", userId1,
                "balance", 1000
        );
        Map<String, Object> cardMapBob = Map.of(
                "number", utility.generateCardNum(16),
                "holderId", userId2,
                "balance", 1000
        );
        String cardJsonAlice = objectMapper.writeValueAsString(cardMapAlice);
        String cardJsonBob = objectMapper.writeValueAsString(cardMapBob);
        MvcResult resultAlice = mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJsonAlice))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult resultBob = mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJsonBob))
                .andExpect(status().isOk())
                .andReturn();

        String jsonAlice = resultBob.getResponse().getContentAsString();
        String jsonBob = resultBob.getResponse().getContentAsString();

        String idAliceCard = objectMapper.readTree(jsonAlice).get("id").asText();
        String idBobCard = objectMapper.readTree(jsonBob).get("id").asText();

        mockMvc.perform(post("/cards/transfer")
                        .param("fromId", idAliceCard)
                        .param("toId", idBobCard)
                        .param("amount", "100")
                        .header("Authorization", "Bearer " + userToken1))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/cards/transfer")
                        .param("fromId", idAliceCard)
                        .param("toId", idBobCard)
                        .param("amount", "100")
                        .header("Authorization", "Bearer " + userToken2))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/cards/transfer")
                        .param("fromId", idBobCard)
                        .param("toId", idAliceCard)
                        .param("amount", "100")
                        .header("Authorization", "Bearer " + userToken1))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/cards/transfer")
                        .param("fromId", idBobCard)
                        .param("toId", idAliceCard)
                        .param("amount", "100")
                        .header("Authorization", "Bearer " + userToken2))
                .andExpect(status().isForbidden());
    }

    @Test
    void transfer_insufficientBalance_shouldReturn400() throws Exception {
        String fromId = utility.createCard(mockMvc, adminToken, utility.generateCardNum(1), userId1, BigDecimal.ONE);
        String toId = utility.createCard(mockMvc, adminToken, utility.generateCardNum(2), userId1, BigDecimal.ZERO);

        utility.createTransferAction(mockMvc, userToken1, fromId, toId, BigDecimal.TEN)
                .andExpect(status().isBadRequest());
    }

    @Test
    void viewBalance_withUserToken_shouldReturnBalance() throws Exception {
        int balance = 1500;
        String id = utility.createCard(mockMvc, adminToken, utility.generateCardNum(17), userId1, BigDecimal.valueOf(balance));

        mockMvc.perform(get("/cards/" + id + "/balance")
                        .header("Authorization", "Bearer " + userToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(balance));
    }

    @Test
    void viewBalance_otherUserCard_shouldReturn403() throws Exception {
        String id = utility.createCard(mockMvc, adminToken, utility.generateCardNum(1), userId2, BigDecimal.valueOf(2000));
        mockMvc.perform(get("/cards/" + id + "/balance")
                        .header("Authorization", "Bearer " + userToken1))
                .andExpect(status().isForbidden());
    }
}
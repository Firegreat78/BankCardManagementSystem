package com.example.bankcards.controller;

import com.example.bankcards.config.AdminConfig;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.util.Utility;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
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
import static org.assertj.core.api.Assertions.assertThat;

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
        utility.createCardAction(mockMvc, adminToken, cardNum, userId1, BigDecimal.TEN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.number").value("**** **** **** " + cardNum.substring(12)));
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
        String id = utility.createCard(mockMvc, adminToken, 1, userId1, BigDecimal.TEN);

        mockMvc.perform(patch("/cards/" + id + "/block")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void deleteCard_withAdminToken_shouldReturn200() throws Exception {
        String id = utility.createCard(mockMvc, adminToken, 1, userId1, BigDecimal.TEN);
        mockMvc.perform(delete("/cards/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void deleteCard_withOwnerUserToken_shouldReturn403() throws Exception {
        String id = utility.createCard(mockMvc, adminToken, 1, userId1, BigDecimal.TEN);
        mockMvc.perform(delete("/cards/" + id)
                        .header("Authorization", "Bearer " + userToken1))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteCard_withNonOwnerUserToken_shouldReturn403() throws Exception {
        String id = utility.createCard(mockMvc, adminToken, 1, userId1, BigDecimal.TEN);

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
    void getCards_withAdminToken_shouldReturnAllCards() throws Exception {
        utility.createCard(
                mockMvc,
                adminToken,
                1,
                userId1,
                BigDecimal.valueOf(100)
        );

        utility.createCard(
                mockMvc,
                adminToken,
                2,
                userId2,
                BigDecimal.valueOf(200)
        );

        mockMvc.perform(get("/cards")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getCards_withUserToken_shouldReturnOnlyOwnCards() throws Exception {
        String ownCardId = utility.createCard(
                mockMvc,
                adminToken,
                3,
                userId1,
                BigDecimal.valueOf(100)
        );

        utility.createCard(
                mockMvc,
                adminToken,
                4,
                userId2,
                BigDecimal.valueOf(200)
        );

        mockMvc.perform(get("/cards")
                        .header("Authorization", "Bearer " + userToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(ownCardId));
    }

    @Test
    void viewOwnCards_withPagination_shouldReturnPaginated() throws Exception {
        for (int i = 0; i < 3; i++) {
            utility.createCard(mockMvc, adminToken, i, userId1, BigDecimal.valueOf(i));
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
        String id = utility.createCard(
                mockMvc, adminToken, 1,
                userId1, BigDecimal.TEN
        );

        mockMvc.perform(patch("/cards/" + id + "/block-request")
                        .header("Authorization", "Bearer " + userToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value(CardStatus.BLOCK_REQUESTED.name()));
    }

    @Test
    void transferBetweenOwnCards_withUserToken_shouldReturn200() throws Exception {
        BigDecimal fromBal = BigDecimal.TEN;
        BigDecimal toBal = BigDecimal.ZERO;
        BigDecimal transferAmt = BigDecimal.ONE;

        String fromId = utility.createCard(
                mockMvc, adminToken, 1, userId1, fromBal
        );
        String toId = utility.createCard(
                mockMvc, adminToken, 2, userId1, toBal
        );
        utility.createTransferAction(mockMvc, userToken1, fromId, toId, transferAmt).andExpect(status().isOk());

        BigDecimal expectedFromBalance = fromBal.subtract(transferAmt);
        BigDecimal expectedToBalance = toBal.add(transferAmt);

        MvcResult fromResult = utility.getCardAction(mockMvc, userToken1, fromId)
                .andExpect(status().isOk())
                .andReturn();

        MvcResult toResult = utility.getCardAction(mockMvc, userToken1, toId)
                .andExpect(status().isOk())
                .andReturn();

        BigDecimal actualFromBalance = new BigDecimal(
                JsonPath.read(
                        fromResult.getResponse().getContentAsString(),
                        "$.balance"
                ).toString()
        );

        BigDecimal actualToBalance = new BigDecimal(
                JsonPath.read(
                        toResult.getResponse().getContentAsString(),
                        "$.balance"
                ).toString()
        );

        assertThat(actualFromBalance).isEqualByComparingTo(expectedFromBalance);
        assertThat(actualToBalance).isEqualByComparingTo(expectedToBalance);
    }

    @Test
    void transferToNotOwnCard_withUserToken_shouldReturn403() throws Exception {
        String idAliceCard = utility.createCard(mockMvc, adminToken, 1, userId1, BigDecimal.TEN);
        String idBobCard = utility.createCard(mockMvc, adminToken, 2, userId2, BigDecimal.TEN);

        utility.createTransferAction(mockMvc, userToken1, idAliceCard, idBobCard, BigDecimal.ONE)
                .andExpect(status().isForbidden());
        utility.createTransferAction(mockMvc, userToken2, idAliceCard, idBobCard, BigDecimal.ONE)
                .andExpect(status().isForbidden());

        utility.createTransferAction(mockMvc, userToken1, idBobCard, idAliceCard, BigDecimal.ONE)
                .andExpect(status().isForbidden());
        utility.createTransferAction(mockMvc, userToken2, idBobCard, idAliceCard, BigDecimal.ONE)
                .andExpect(status().isForbidden());
    }

    @Test
    void transfer_insufficientBalance_shouldReturn400() throws Exception {
        String fromId = utility.createCard(mockMvc, adminToken, 1, userId1, BigDecimal.ONE);
        String toId = utility.createCard(mockMvc, adminToken, 2, userId1, BigDecimal.ZERO);

        utility.createTransferAction(mockMvc, userToken1, fromId, toId, BigDecimal.TEN)
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestUnblockCard_withUserToken_shouldReturn200() throws Exception {
        String id = utility.createCard(
                mockMvc, adminToken, 1,
                userId1, BigDecimal.TEN
        );

        utility.blockCardAction(mockMvc, adminToken, id)
                .andExpect(status().isOk());

        utility.requestCardUnblockAction(mockMvc, userToken1, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value(CardStatus.UNBLOCK_REQUESTED.name()));
    }

    @Test
    void blockRequestedCard_withAdminToken_shouldBecomeBlocked() throws Exception {
        String id = utility.createCard(
                mockMvc, adminToken, 1,
                userId1, BigDecimal.TEN
        );

        utility.requestCardBlockAction(mockMvc, userToken1, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value(CardStatus.BLOCK_REQUESTED.name()));

        utility.blockCardAction(mockMvc, adminToken, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value(CardStatus.BLOCKED.name()));
    }

    @Test
    void activateBlockRequestedCard_withAdminToken_shouldBecomeActive() throws Exception {
        String id = utility.createCard(
                mockMvc, adminToken, 1,
                userId1, BigDecimal.TEN
        );

        utility.requestCardBlockAction(mockMvc, userToken1, id)
                .andExpect(status().isOk());

        utility.activateCardAction(mockMvc, adminToken, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value(CardStatus.ACTIVE.name()));
    }

    @Test
    void activateUnblockRequestedCard_withAdminToken_shouldBecomeActive() throws Exception {
        String id = utility.createCard(
                mockMvc, adminToken, 1,
                userId1, BigDecimal.TEN
        );

        utility.blockCardAction(mockMvc, adminToken, id)
                .andExpect(status().isOk());

        utility.requestCardUnblockAction(mockMvc, userToken1, id)
                .andExpect(status().isOk());

        utility.activateCardAction(mockMvc, adminToken, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value(CardStatus.ACTIVE.name()));
    }

    @Test
    void blockUnblockRequestedCard_withAdminToken_shouldBecomeBlocked() throws Exception {
        String id = utility.createCard(
                mockMvc, adminToken, 1,
                userId1, BigDecimal.TEN
        );

        utility.blockCardAction(mockMvc, adminToken, id)
                .andExpect(status().isOk());

        utility.requestCardUnblockAction(mockMvc, userToken1, id)
                .andExpect(status().isOk());

        utility.blockCardAction(mockMvc, adminToken, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value(CardStatus.BLOCKED.name()));
    }
}
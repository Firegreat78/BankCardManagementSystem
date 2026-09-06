package com.example.bankcards.controller;

import com.example.bankcards.util.Utility;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("unused")
class AdminCardControllerTest extends com.example.bankcards.IntegrationTest {

    @Autowired
    private Utility utility;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userId1;

    private static final String USER1_USERNAME = "alice";
    private static final String USER1_PASSWORD = "alice123";
    private static final String USER2_USERNAME = "bob";
    private static final String USER2_PASSWORD = "bob123";

    @BeforeEach
    public void setUp() throws Exception {
        adminToken = utility.loginAdmin(mockMvc);
        userId1 = utility.registerUser(mockMvc, adminToken, USER1_USERNAME, USER1_PASSWORD);
    }

    @Test
    void createCard_shouldReturnCard() throws Exception {
        String num = utility.generateCardNum(1);
        BigDecimal balance = BigDecimal.TEN;

        utility.createCardAction(mockMvc, adminToken, 1, userId1, balance)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.number").value("**** **** **** " + num.substring(12)))
                .andExpect(jsonPath("$.holderId").value(userId1))
                .andExpect(jsonPath("$.balance").value(balance));
    }

    @Test
    void createCard_negativeBalance_shouldReturn400() throws Exception {
        utility.createCardAction(mockMvc, adminToken, 1, userId1, BigDecimal.valueOf(-1))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCard_invalidNumberLength_shouldReturn400() throws Exception {
        utility.createCardAction(mockMvc, adminToken, "1".repeat(15), userId1, BigDecimal.ZERO)
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCard_invalidNumberLength2_shouldReturn400() throws Exception {
        utility.createCardAction(mockMvc, adminToken, "1".repeat(17), userId1, BigDecimal.ZERO)
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCard_numberWithLetters_shouldReturn400() throws Exception {
        String invalid_number = "1".repeat(7) + "A" + "1".repeat(8);
        utility.createCardAction(mockMvc, adminToken, invalid_number, userId1, BigDecimal.ZERO)
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCard_duplicateNumber_shouldReturn409() throws Exception {
        utility.createCardAction(mockMvc, adminToken, 1, userId1, BigDecimal.ZERO).andExpect(status().isOk());
        utility.createCardAction(mockMvc, adminToken, 1, userId1, BigDecimal.ZERO).andExpect(status().isConflict());
    }

    @Test
    void createCard_sameHolderDifferentNumber_shouldSucceed() throws Exception {
        utility.createCardAction(mockMvc, adminToken, 1, userId1, BigDecimal.ZERO).andExpect(status().isOk());
        utility.createCardAction(mockMvc, adminToken, 2, userId1, BigDecimal.ZERO).andExpect(status().isOk());
    }

    @Test
    void deleteCard_shouldReturn200() throws Exception {
        String id = utility.createCard(mockMvc, adminToken, 1, userId1, BigDecimal.ZERO);

        utility.deleteCardAction(mockMvc, adminToken, id).andExpect(status().isOk());
        utility.getCardAction(mockMvc, adminToken, id).andExpect(status().isNotFound());
    }

    @Test
    void deleteCard_nonExistent_shouldReturn404() throws Exception {
        utility.deleteCardAction(mockMvc, adminToken, "1").andExpect(status().isNotFound());
    }

    @Test
    void deleteCard_afterDeletion_fetchShouldReturn404() throws Exception {
        String card_id = utility.createCard(mockMvc, adminToken, 1, userId1, BigDecimal.ZERO);

        utility.getCardAction(mockMvc, adminToken, card_id).andExpect(status().isOk());
        utility.deleteCardAction(mockMvc, adminToken, card_id);
        utility.getCardAction(mockMvc, adminToken, card_id).andExpect(status().isNotFound());
    }

    @Test
    void getCards_shouldReturnList() throws Exception {
        final int AMOUNT = 3;
        for (int i = 0; i < AMOUNT; i++) {
            String cardNumber = utility.generateCardNum(i);
            String cardId = utility.createCard(mockMvc, adminToken, i, userId1, BigDecimal.valueOf(i));
        }

        mockMvc.perform(get("/cards")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(AMOUNT));
    }

    @Test
    void getCards_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/cards"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCards_filteredByLast4_shouldReturnOnlyMatchingCard() throws Exception {
        utility.createCard(mockMvc, adminToken, 1, userId1, BigDecimal.ZERO);
        utility.createCard(mockMvc, adminToken, 2, userId1, BigDecimal.ZERO);

        mockMvc.perform(get("/cards")
                        .param("last4", "0002")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].number").value("**** **** **** 0002"));
    }

    @Test
    void getCards_filteredByStatus_shouldReturnOnlyMatchingCard() throws Exception {
        String blockedId = utility.createCard(mockMvc, adminToken, 1, userId1, BigDecimal.ZERO);
        utility.createCard(mockMvc, adminToken, 2, userId1, BigDecimal.ZERO);
        utility.blockCardAction(mockMvc, adminToken, blockedId).andExpect(status().isOk());

        mockMvc.perform(get("/cards")
                        .param("status", "BLOCKED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(blockedId));
    }

    @Test
    void getCards_invalidLast4_shouldReturn400WithMessage() throws Exception {
        mockMvc.perform(get("/cards")
                        .param("last4", "12")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.last4").value("last4 must be exactly 4 digits"));
    }

    @Test
    void getCards_unknownStatusValue_shouldReturn400NotServerError() throws Exception {
        mockMvc.perform(get("/cards")
                        .param("status", "NOT_A_STATUS")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCards_zeroSize_shouldReturn400NotServerError() throws Exception {
        mockMvc.perform(get("/cards")
                        .param("size", "0")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCard_invalidNumber_shouldReturnFieldErrorInBody() throws Exception {
        utility.createCardAction(mockMvc, adminToken, "1".repeat(15), userId1, BigDecimal.ZERO)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.number").value("Card number must be exactly 16 digits"));
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
        int cardCount = 5;
        int pageSize = 2;

        for (int i = 1; i <= cardCount; i++) {
            utility.createCard(
                    mockMvc,
                    adminToken,
                    i,
                    userId1,
                    BigDecimal.valueOf(100L * i)
            );
        }

        ResultActions result = mockMvc.perform(get("/cards")
                        .param("size", String.valueOf(pageSize))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(pageSize));

        for (int i = 0; i < pageSize; i++) {
            String num = utility.generateCardNum(i + 1);
            result.andExpect(
                    jsonPath("$[" + i + "].number").value("**** **** **** " + num.substring(12))
            );
        }
    }

    @Test
    void getCards_withPagination_shouldReturnPaginated() throws Exception {
        int cardCount = 5;
        int pageSize = 2;

        for (int i = 1; i <= cardCount; i++) {
            utility.createCard(
                    mockMvc,
                    adminToken,
                    i,
                    userId1,
                    BigDecimal.valueOf(100L * i)
            );
        }
        int pageCount = (cardCount + pageSize - 1) / pageSize;

        for (int page = 0; page < pageCount; page++) {
            int expectedSize = Math.min(
                    pageSize,
                    cardCount - page * pageSize
            );
            mockMvc.perform(get("/cards")
                            .param("page", String.valueOf(page))
                            .param("size", String.valueOf(pageSize))
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(expectedSize));
        }
    }

    @Test
    void getCards_withPagination_onLargeDataset_shouldReturnOnlyRequestedPage() throws Exception {
        int cardCount = 200;
        int pageSize = 10;

        for (int i = 1; i <= cardCount; i++) {
            utility.createCard(
                    mockMvc,
                    adminToken,
                    i,
                    userId1,
                    BigDecimal.valueOf(100L * i)
            );
        }

        int lastPage = (cardCount / pageSize) - 1;
        mockMvc.perform(get("/cards")
                        .param("page", String.valueOf(lastPage))
                        .param("size", String.valueOf(pageSize))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(pageSize))
                .andExpect(jsonPath("$[0].number")
                        .value("**** **** **** " + utility.generateCardNum(cardCount - pageSize + 1).substring(12)));
    }
}
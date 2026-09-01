package com.example.bankcards.controller;

import com.example.bankcards.util.Utility;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Locale;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("unused")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AdminCardControllerTest {

    @Autowired
    private Utility utility;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userId1;
    private String userId2;

    private static final String USER1_USERNAME = "alice";
    private static final String USER1_PASSWORD = "alice123".toLowerCase(Locale.ROOT);
    private static final String USER2_USERNAME = "bob";
    private static final String USER2_PASSWORD = "bob123";

    @BeforeEach
    public void setUp() throws Exception {
        adminToken = utility.loginAdmin(mockMvc);
        if (userId1 == null) userId1 = utility.registerUser(mockMvc, adminToken, USER1_USERNAME, USER1_PASSWORD);
        if (userId2 == null) userId2 = utility.registerUser(mockMvc, adminToken, USER2_USERNAME, USER2_PASSWORD);
    }

    @Test
    void createCard_shouldReturnCard() throws Exception {
        String num = utility.generateCardNum(1);
        BigDecimal balance = BigDecimal.TEN;

        utility.createCardAction(mockMvc, adminToken, num, userId1, balance)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.number").value(num))
                .andExpect(jsonPath("$.holderId").value(userId1))
                .andExpect(jsonPath("$.balance").value(balance));
    }

    @Test
    void createCard_negativeBalance_shouldReturn400() throws Exception {
        String num = utility.generateCardNum(1);
        utility.createCardAction(mockMvc, adminToken, num, userId1, BigDecimal.valueOf(-1))
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
        String card_num = utility.generateCardNum(1);

        utility.createCardAction(mockMvc, adminToken, card_num, userId1, BigDecimal.ZERO).andExpect(status().isOk());
        utility.createCardAction(mockMvc, adminToken, card_num, userId1, BigDecimal.ZERO).andExpect(status().isConflict());
    }

    @Test
    void createCard_sameHolderDifferentNumber_shouldSucceed() throws Exception {
        String card_num1 = utility.generateCardNum(1);
        String card_num2 = utility.generateCardNum(2);

        utility.createCardAction(mockMvc, adminToken, card_num1, userId1, BigDecimal.ZERO).andExpect(status().isOk());
        utility.createCardAction(mockMvc, adminToken, card_num2, userId1, BigDecimal.ZERO).andExpect(status().isOk());
    }

    @Test
    void deleteCard_shouldReturn200() throws Exception {
        String id = utility.createCard(mockMvc, adminToken, utility.generateCardNum(1), userId1, BigDecimal.ZERO);

        utility.deleteCardAction(mockMvc, adminToken, id).andExpect(status().isOk());
        utility.getCardAction(mockMvc, adminToken, id).andExpect(status().isNotFound());
    }

    @Test
    void deleteCard_nonExistent_shouldReturn404() throws Exception {
        utility.deleteCardAction(mockMvc, adminToken, "1").andExpect(status().isNotFound());
    }

    @Test
    void deleteCard_afterDeletion_fetchShouldReturn404() throws Exception {
        String num = utility.generateCardNum(1);
        String card_id = utility.createCard(mockMvc, adminToken, num, userId1, BigDecimal.ZERO);

        utility.getCardAction(mockMvc, adminToken, card_id).andExpect(status().isOk());
        utility.deleteCardAction(mockMvc, adminToken, card_id);
        utility.getCardAction(mockMvc, adminToken, card_id).andExpect(status().isNotFound());
    }

    @Test
    void getCards_shouldReturnList() throws Exception {
        final int AMOUNT = 3;
        ArrayList<String> cardNumbers = new ArrayList<>(AMOUNT);
        ArrayList<String> cardIds = new ArrayList<>(AMOUNT);

        for (int i = 0; i < AMOUNT; i++) {
            String cardNumber = utility.generateCardNum(i);
            String cardId = utility.createCard(mockMvc, adminToken, cardNumber, userId1, BigDecimal.valueOf(i));
            cardNumbers.add(cardNumber);
            cardIds.add(cardId);
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
    void getCards_onlyPageWithoutSize_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/cards")
                        .param("page", "0")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCards_onlySizeWithoutPage_shouldReturnFirstPage() throws Exception {
        for (int i = 1; i <= 5; i++) {
            utility.createCard(mockMvc, adminToken, utility.generateCardNum(i), userId1, BigDecimal.valueOf(100 * i));
        }

        mockMvc.perform(get("/cards")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].number").value(utility.generateCardNum(1)))
                .andExpect(jsonPath("$[1].number").value(utility.generateCardNum(2)));
    }

    @Test
    void getCards_withPagination_shouldReturnPaginated() throws Exception {
        for (int i = 1; i <= 5; i++) {
            utility.createCard(mockMvc, adminToken, utility.generateCardNum(i), userId1, BigDecimal.valueOf(100 * i));
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
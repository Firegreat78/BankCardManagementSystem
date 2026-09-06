package com.example.bankcards.service;

import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.util.Utility;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("unused")
class CardExpirationTest extends com.example.bankcards.IntegrationTest {

    @Autowired
    Utility utility;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    private String adminToken;
    private String userToken1;
    private String userId1;

    private static final String USER1_USERNAME = "alice";
    private static final String USER1_PASSWORD = "alice123";

    @BeforeEach
    public void setUp() throws Exception {
        adminToken = utility.loginAdmin(mockMvc);
        userId1 = utility.registerUser(mockMvc, adminToken, USER1_USERNAME, USER1_PASSWORD);
        userToken1 = utility.loginUser(mockMvc, USER1_USERNAME, USER1_PASSWORD);
    }

    @Test
    void getCard_pastExpirationDate_shouldReportExpiredStatus() throws Exception {
        String cardId = utility.createCardAction(
                        mockMvc,
                        adminToken,
                        1,
                        userId1,
                        BigDecimal.TEN,
                        LocalDate.now().minusDays(1)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = objectMapper.readTree(cardId).get("id").asText();

        utility.getCardAction(mockMvc, adminToken, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRED"));
    }

    @Test
    void getCard_futureExpirationDate_shouldStayActive() throws Exception {
        String id = utility.createCard(
                mockMvc,
                adminToken,
                1,
                userId1,
                BigDecimal.TEN
        );

        utility.getCardAction(mockMvc, adminToken, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void activateExpiredCard_shouldReturnBadRequest() throws Exception {
        String response = utility.createCardAction(
                        mockMvc,
                        adminToken,
                        1,
                        userId1,
                        BigDecimal.TEN,
                        LocalDate.now().minusDays(1)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = objectMapper.readTree(response).get("id").asText();

        utility.activateCardAction(mockMvc, adminToken, id)
                .andExpect(status().isBadRequest());
    }

    @Test
    void listCards_pastExpirationDate_shouldReportExpiredStatus() throws Exception {
        String response = utility.createCardAction(
                        mockMvc,
                        adminToken,
                        1,
                        userId1,
                        BigDecimal.TEN,
                        LocalDate.now().minusDays(1)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/cards")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].status").value("EXPIRED"));
    }

    @Test
    void transferFromExpiredCard_shouldReturnBadRequest() throws Exception {
        String fromId = objectMapper.readTree(
                utility.createCardAction(
                                mockMvc,
                                adminToken,
                                1,
                                userId1,
                                BigDecimal.TEN,
                                LocalDate.now().minusDays(1)
                        )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        ).get("id").asText();
        String toId = utility.createCard(
                mockMvc,
                adminToken,
                2,
                userId1,
                BigDecimal.ZERO
        );

        utility.createTransferAction(mockMvc, userToken1, fromId, toId, BigDecimal.ONE)
                .andExpect(status().isBadRequest());
    }

    @Test
    void transferFromBlockedCard_shouldReturnBadRequest() throws Exception {
        String fromId = utility.createCard(
                mockMvc,
                adminToken,
                1,
                userId1,
                BigDecimal.TEN
        );
        String toId = utility.createCard(
                mockMvc,
                adminToken,
                2,
                userId1,
                BigDecimal.ZERO
        );
        utility.blockCardAction(mockMvc, adminToken, fromId)
                .andExpect(status().isOk());

        utility.createTransferAction(mockMvc, userToken1, fromId, toId, BigDecimal.ONE)
                .andExpect(status().isBadRequest());
    }
}

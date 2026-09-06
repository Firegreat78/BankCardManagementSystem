package com.example.bankcards.service;

import com.example.bankcards.util.Utility;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("unused")
class CardSecurityTest extends com.example.bankcards.IntegrationTest {

    @Autowired
    Utility utility;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    private String adminToken;
    private String userId1;

    private static final String USER1_USERNAME = "alice";
    private static final String USER1_PASSWORD = "alice123";

    @BeforeEach
    public void setUp() throws Exception {
        adminToken = utility.loginAdmin(mockMvc);
        userId1 = utility.registerUser(mockMvc, adminToken, USER1_USERNAME, USER1_PASSWORD);
    }

    @Test
    void createCard_shouldReturnMaskedNumber() throws Exception {
        String plainNumber = utility.generateCardNum(1);

        utility.createCardAction(mockMvc, adminToken, 1, userId1, BigDecimal.TEN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("**** **** **** 0001"))
                .andExpect(jsonPath("$.number").value(org.hamcrest.Matchers.not(plainNumber)));
    }

    @Test
    void getCard_shouldReturnMaskedNumber() throws Exception {
        String id = utility.createCard(mockMvc, adminToken, 1, userId1, BigDecimal.TEN);

        utility.getCardAction(mockMvc, adminToken, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("**** **** **** 0001"));
    }

    @Test
    void listCards_shouldReturnMaskedNumbers() throws Exception {
        utility.createCard(mockMvc, adminToken, 1, userId1, BigDecimal.TEN);

        mockMvc.perform(get("/cards")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("**** **** **** 0001"));
    }

    @Test
    void cardNumber_shouldBeEncryptedAtRest() throws Exception {
        String plainNumber = utility.generateCardNum(1);
        String id = utility.createCard(mockMvc, adminToken, 1, userId1, BigDecimal.TEN);

        // Raw SQL bypasses the persistence context, so push the insert to the database first.
        entityManager.flush();

        String rawColumnValue = jdbcTemplate.queryForObject(
                "SELECT number FROM card WHERE id = ?",
                String.class,
                id
        );

        assertThat(rawColumnValue).isNotEqualTo(plainNumber);
    }

    @Test
    void createCard_shouldNotExposeNumberHash() throws Exception {
        utility.createCardAction(mockMvc, adminToken, 1, userId1, BigDecimal.TEN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numberHash").doesNotExist());
    }

    @Test
    void createCard_duplicateNumber_stillDetectedAfterEncryption() throws Exception {
        utility.createCardAction(mockMvc, adminToken, 1, userId1, BigDecimal.TEN)
                .andExpect(status().isOk());

        utility.createCardAction(mockMvc, adminToken, 1, userId1, BigDecimal.TEN)
                .andExpect(status().isConflict());
    }
}

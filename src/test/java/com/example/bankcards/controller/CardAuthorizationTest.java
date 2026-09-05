package com.example.bankcards.controller;

import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.util.Utility;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers access control for single-card read, update, and direct
 * block/activate vs. block-request/unblock-request: regular users may only
 * touch their own cards, admins may touch any card, and direct block/activate
 * are admin-only regardless of ownership.
 */
@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("unused")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ActiveProfiles("test")
class CardAuthorizationTest {

    @Autowired
    private Utility utility;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userId1;
    private String userToken1;
    private String userToken2;

    private static final String USER1_USERNAME = "alice";
    private static final String USER1_PASSWORD = "alice123";
    private static final String USER2_USERNAME = "bob";
    private static final String USER2_PASSWORD = "bob123";

    @BeforeEach
    void setUp() throws Exception {
        adminToken = utility.loginAdmin(mockMvc);
        userId1 = utility.registerUser(mockMvc, adminToken, USER1_USERNAME, USER1_PASSWORD);
        utility.registerUser(mockMvc, adminToken, USER2_USERNAME, USER2_PASSWORD);
        userToken1 = utility.loginUser(mockMvc, USER1_USERNAME, USER1_PASSWORD);
        userToken2 = utility.loginUser(mockMvc, USER2_USERNAME, USER2_PASSWORD);
    }

    @Test
    void getById_owner_shouldReturn200() throws Exception {
        String id = utility.createCard(mockMvc, adminToken, 1, userId1, BigDecimal.TEN);

        utility.getCardAction(mockMvc, userToken1, id)
                .andExpect(status().isOk());
    }

    @Test
    void getById_nonOwnerUser_shouldReturn403() throws Exception {
        String id = utility.createCard(mockMvc, adminToken, 1, userId1, BigDecimal.TEN);

        utility.getCardAction(mockMvc, userToken2, id)
                .andExpect(status().isForbidden());
    }

    @Test
    void getById_admin_shouldReturn200ForAnyCard() throws Exception {
        String id = utility.createCard(mockMvc, adminToken, 1, userId1, BigDecimal.TEN);

        utility.getCardAction(mockMvc, adminToken, id)
                .andExpect(status().isOk());
    }

    @Test
    void update_withUserToken_evenAsOwner_shouldReturn403() throws Exception {
        String id = utility.createCard(mockMvc, adminToken, 1, userId1, BigDecimal.TEN);

        mockMvc.perform(put("/cards/" + id)
                        .header("Authorization", "Bearer " + userToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "number", utility.generateCardNum(1),
                                "holderId", userId1,
                                "balance", BigDecimal.valueOf(999),
                                "expirationDate", LocalDate.now().plusYears(1)
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_withAdminToken_shouldSucceed() throws Exception {
        String id = utility.createCard(mockMvc, adminToken, 1, userId1, BigDecimal.TEN);

        mockMvc.perform(put("/cards/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "number", utility.generateCardNum(1),
                                "holderId", userId1,
                                "balance", BigDecimal.valueOf(999),
                                "expirationDate", LocalDate.now().plusYears(1)
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(999));
    }

    @Test
    void update_withAdminToken_cannotBypassStateMachineViaStatusField() throws Exception {
        String id = utility.createCard(mockMvc, adminToken, 1, userId1, BigDecimal.TEN);

        mockMvc.perform(put("/cards/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "number", utility.generateCardNum(1),
                                "holderId", userId1,
                                "balance", BigDecimal.TEN,
                                "status", CardStatus.BLOCKED.name(),
                                "expirationDate", LocalDate.now().plusYears(1)
                        ))))
                .andExpect(status().isOk());

        utility.getCardAction(mockMvc, adminToken, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(CardStatus.ACTIVE.name()));
    }

    @Test
    void block_withUserToken_evenAsOwner_shouldReturn403() throws Exception {
        String id = utility.createCard(mockMvc, adminToken, 1, userId1, BigDecimal.TEN);

        utility.blockCardAction(mockMvc, userToken1, id)
                .andExpect(status().isForbidden());
    }

    @Test
    void activate_withUserToken_evenAsOwner_shouldReturn403() throws Exception {
        String id = utility.createCard(mockMvc, adminToken, 1, userId1, BigDecimal.TEN);
        utility.blockCardAction(mockMvc, adminToken, id).andExpect(status().isOk());

        utility.activateCardAction(mockMvc, userToken1, id)
                .andExpect(status().isForbidden());
    }

    @Test
    void requestBlock_nonOwnerUser_shouldReturn403() throws Exception {
        String id = utility.createCard(mockMvc, adminToken, 1, userId1, BigDecimal.TEN);

        utility.requestCardBlockAction(mockMvc, userToken2, id)
                .andExpect(status().isForbidden());
    }

    @Test
    void requestUnblock_nonOwnerUser_shouldReturn403() throws Exception {
        String id = utility.createCard(mockMvc, adminToken, 1, userId1, BigDecimal.TEN);
        utility.blockCardAction(mockMvc, adminToken, id).andExpect(status().isOk());

        utility.requestCardUnblockAction(mockMvc, userToken2, id)
                .andExpect(status().isForbidden());
    }

    @Test
    void requestBlock_owner_shouldReturn200() throws Exception {
        String id = utility.createCard(mockMvc, adminToken, 1, userId1, BigDecimal.TEN);

        utility.requestCardBlockAction(mockMvc, userToken1, id)
                .andExpect(status().isOk());
    }

    @Test
    void create_clientSuppliedIdAndStatus_shouldBeIgnored() throws Exception {
        mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "id", "client-chosen-id",
                                "number", utility.generateCardNum(1),
                                "holderId", userId1,
                                "balance", BigDecimal.TEN,
                                "status", CardStatus.BLOCKED.name(),
                                "expirationDate", LocalDate.now().plusYears(1)
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not("client-chosen-id")))
                .andExpect(jsonPath("$.status").value(CardStatus.ACTIVE.name()));
    }
}

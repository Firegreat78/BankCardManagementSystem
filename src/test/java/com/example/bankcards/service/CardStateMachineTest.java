package com.example.bankcards.service;

import com.example.bankcards.config.AdminConfig;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.util.Utility;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@SuppressWarnings("unused")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CardStateMachineTest {

    @Autowired
    Utility utility;

    @Autowired
    private AdminConfig adminConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    private String adminToken = null;

    private String userId1 = null;
    private String userToken1 = null;

    private static final String USER1_USERNAME = "alice";
    private static final String USER1_PASSWORD = "alice123";

    @BeforeEach
    public void setUp() throws Exception {
        adminToken = utility.loginAdmin(mockMvc);
        userId1 = utility.registerUser(mockMvc, adminToken, USER1_USERNAME, USER1_PASSWORD);
        userToken1 = utility.loginUser(mockMvc, USER1_USERNAME, USER1_PASSWORD);
    }

    @Test
    void validTransitions_shouldBeAllowed() {
        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.ACTIVE,
                CardStatus.BLOCK_REQUESTED
        )).isTrue();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.ACTIVE,
                CardStatus.BLOCKED
        )).isTrue();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.BLOCK_REQUESTED,
                CardStatus.ACTIVE
        )).isTrue();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.BLOCK_REQUESTED,
                CardStatus.BLOCKED
        )).isTrue();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.BLOCKED,
                CardStatus.UNBLOCK_REQUESTED
        )).isTrue();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.UNBLOCK_REQUESTED,
                CardStatus.ACTIVE
        )).isTrue();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.UNBLOCK_REQUESTED,
                CardStatus.BLOCKED
        )).isTrue();
    }

    @Test
    void invalidTransitions_shouldNotBeAllowed() {
        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.ACTIVE,
                CardStatus.ACTIVE
        )).isFalse();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.ACTIVE,
                CardStatus.UNBLOCK_REQUESTED
        )).isFalse();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.BLOCK_REQUESTED,
                CardStatus.BLOCK_REQUESTED
        )).isFalse();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.BLOCK_REQUESTED,
                CardStatus.UNBLOCK_REQUESTED
        )).isFalse();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.BLOCKED,
                CardStatus.BLOCK_REQUESTED
        )).isFalse();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.BLOCKED,
                CardStatus.BLOCKED
        )).isFalse();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.BLOCKED,
                CardStatus.ACTIVE
        )).isFalse();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.UNBLOCK_REQUESTED,
                CardStatus.UNBLOCK_REQUESTED
        )).isFalse();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.UNBLOCK_REQUESTED,
                CardStatus.BLOCK_REQUESTED
        )).isFalse();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.EXPIRED,
                CardStatus.EXPIRED
        )).isFalse();
    }

    @Test
    void requestUnblockActiveCard_withUserToken_shouldReturnBadRequest() throws Exception {
        String id = utility.createCard(
                mockMvc,
                adminToken,
                1,
                userId1,
                BigDecimal.TEN
        );

        utility.requestCardUnblockAction(mockMvc, userToken1, id)
                .andExpect(status().isBadRequest());
    }
}

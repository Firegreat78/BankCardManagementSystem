package com.example.bankcards.controller;

import com.example.bankcards.entity.Card;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CardController.class)
@AutoConfigureMockMvc(addFilters = false)  // ← bypass security
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Card card;

    @BeforeEach
    void setUp() {
        card = new Card();
        card.setId("1");
        card.setNumber("4111111111111111");
        card.setHolder("John Doe");
        card.setBalance(BigDecimal.valueOf(1000));
    }

    @Test
    void createCard_shouldReturnCard() throws Exception {
        mockMvc.perform(post("/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(card)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.balance").value(1000));
    }

    @Test
    void createCard_negativeBalance_shouldReturn400() throws Exception {
        card.setBalance(BigDecimal.valueOf(-100));
        mockMvc.perform(post("/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(card)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transfer_insufficientBalance_shouldReturn400() throws Exception {
        // Create first card
        mockMvc.perform(post("/cards")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(card)));

        Card card2 = new Card();
        card2.setId("2");
        card2.setNumber("4222222222222222");
        card2.setHolder("Jane Smith");
        card2.setBalance(BigDecimal.valueOf(500));
        mockMvc.perform(post("/cards")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(card2)));

        // Try overdraft
        mockMvc.perform(post("/cards/transfer")
                        .param("fromId", "1")
                        .param("toId", "2")
                        .param("amount", "2000"))
                .andExpect(status().isBadRequest());
    }
}
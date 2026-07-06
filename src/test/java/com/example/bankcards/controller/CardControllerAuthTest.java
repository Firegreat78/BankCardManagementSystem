package com.example.bankcards.controller;

import com.example.bankcards.util.Utility;
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

import java.util.Map;

import static com.example.bankcards.util.Utility.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SuppressWarnings("unused")
class CardControllerAuthTest {
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
        Map<String, Object> adminLoginMap = Map.of("username", ADMIN_USERNAME, "password", ADMIN_PASSWORD);
        String loginJson = objectMapper.writeValueAsString(adminLoginMap);
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        adminToken = objectMapper.readTree(json).get("token").asText();

        // Create test users if not created yet
        if (userId1 == null)
            userId1 = mockRegisterUser(adminToken, mockMvc, objectMapper, USER1_USERNAME, USER1_PASSWORD);
        if (userId2 == null)
            userId2 = mockRegisterUser(adminToken, mockMvc, objectMapper, USER2_USERNAME, USER2_PASSWORD);

        System.out.println("userId1: " + userId1);
        System.out.println("userId2: " + userId2);

        // get user tokens
        if (userToken1 == null)
            userToken1 = getUserToken(mockMvc, objectMapper, USER1_USERNAME, USER1_PASSWORD);
        if (userToken2 == null)
            userToken2 = getUserToken(mockMvc, objectMapper, USER2_USERNAME, USER2_PASSWORD);

        System.out.println("userToken1: " + userToken1);
        System.out.println("userToken2: " + userToken2);
    }

    @Test
    void createCard_withoutToken_shouldReturn401() throws Exception {
        Map<String, Object> cardMap = Map.of(
                "number", cardNum(1),
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
        String cardNum = cardNum(1);
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
                .andDo(print())
                .andExpect(jsonPath("$.number").value(cardNum));
    }

    @Test
    void createCard_withUserToken_shouldReturn403() throws Exception {
        String userToken = getUserToken(mockMvc, objectMapper, "alice", "alice123");
        Map<String, Object> cardMap = Map.of(
                "number", cardNum(2),
                "holderId", userId1,
                "balance", 500
        );
        String cardJson = objectMapper.writeValueAsString(cardMap);
        mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCard_negativeBalance_shouldReturn400() throws Exception {
        Map<String, Object> cardMap = Map.of(
                "number", cardNum(3),
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
                "number", cardNum(4),
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
                "number", cardNum(5),
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
                "number", cardNum(6),
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
        String userToken1 = getUserToken(mockMvc, objectMapper, "alice", "alice123");
        String userToken2 = getUserToken(mockMvc, objectMapper, "bob", "bob123");

        Map<String, Object> cardMap = Map.of(
                "number", cardNum(7),
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
        String userToken = getUserToken(mockMvc, objectMapper, "alice", "alice123");
        mockMvc.perform(get("/cards/all")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewOwnCards_withUserToken_shouldReturnOnlyOwned() throws Exception {
        String userToken = getUserToken(mockMvc, objectMapper, "alice", "alice123");

        Map<String, Object> cardMap = Map.of(
                "number", cardNum(8),
                "holderId", userId1,
                "balance", 1000,
                "ownerId", "alice"
        );
        String cardJson = objectMapper.writeValueAsString(cardMap);
        mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson))
                .andExpect(status().isOk());

        mockMvc.perform(get("/cards")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].ownerId").value("alice"));
    }

    @Test
    void viewOwnCards_withPagination_shouldReturnPaginated() throws Exception {
        String userToken = getUserToken(mockMvc, objectMapper, "alice", "alice123");

        for (int i = 0; i < 3; i++) {
            Map<String, Object> cardMap = Map.of(
                    "number", cardNum(9 + i),
                    "holderId", userId1,
                    "balance", 1000,
                    "ownerId", "alice"
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
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/cards")
                        .param("page", "1")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void requestBlockCard_withUserToken_shouldReturn200() throws Exception {
        String userToken = getUserToken(mockMvc, objectMapper, "alice", "alice123");

        Map<String, Object> cardMap = Map.of(
                "number", cardNum(12),
                "holderId", userId1,
                "balance", 1000,
                "ownerId", "alice"
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
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCK_REQUESTED"));
    }

    @Test
    void transferBetweenOwnCards_withUserToken_shouldReturn200() throws Exception {
        String userToken = getUserToken(mockMvc, objectMapper, "alice", "alice123");

        Map<String, Object> cardMap1 = Map.of(
                "number", cardNum(13),
                "holderId", userId1,
                "balance", 1000,
                "ownerId", "alice"
        );
        Map<String, Object> cardMap2 = Map.of(
                "number", cardNum(14),
                "holderId", userId1,
                "balance", 100,
                "ownerId", "alice"
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
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    void transferToNotOwnCard_withUserToken_shouldReturn403() throws Exception {
        String userToken = getUserToken(mockMvc, objectMapper, "alice", "alice123");

        Map<String, Object> cardMap = Map.of(
                "number", cardNum(15),
                "holderId", userId2,
                "balance", 1000,
                "ownerId", "bob"
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

        mockMvc.perform(post("/cards/transfer")
                        .param("fromId", id)
                        .param("toId", id)
                        .param("amount", "100")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void transfer_insufficientBalance_shouldReturn400() throws Exception {
        String userToken = getUserToken(mockMvc, objectMapper, "alice", "alice123");

        Map<String, Object> cardMap = Map.of(
                "number", cardNum(16),
                "holderId", userId1,
                "balance", 50,
                "ownerId", "alice"
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

        mockMvc.perform(post("/cards/transfer")
                        .param("fromId", id)
                        .param("toId", id)
                        .param("amount", "100")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void viewBalance_withUserToken_shouldReturnBalance() throws Exception {
        String userToken = getUserToken(mockMvc, objectMapper, "alice", "alice123");

        Map<String, Object> cardMap = Map.of(
                "number", cardNum(17),
                "holderId", userId1,
                "balance", 1500,
                "ownerId", "alice"
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

        mockMvc.perform(get("/cards/" + id + "/balance")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1500));
    }

    @Test
    void viewBalance_otherUserCard_shouldReturn403() throws Exception {
        String userToken = getUserToken(mockMvc, objectMapper, "alice", "alice123");

        Map<String, Object> cardMap = Map.of(
                "number", cardNum(18),
                "holderId", userId2,
                "balance", 2000,
                "ownerId", "bob"
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

        mockMvc.perform(get("/cards/" + id + "/balance")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
}
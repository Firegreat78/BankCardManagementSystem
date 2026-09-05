package com.example.bankcards.dto;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardNumberMaskingSerializer;
import com.example.bankcards.entity.CardStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CardResponse {

    private String id;

    @JsonSerialize(using = CardNumberMaskingSerializer.class)
    private String number;

    private String holderId;

    private CardStatus status;

    private BigDecimal balance;

    private LocalDate expirationDate;

    public static CardResponse from(Card card) {
        CardResponse response = new CardResponse();
        response.setId(card.getId());
        response.setNumber(card.getNumber());
        response.setHolderId(card.getHolderId());
        response.setStatus(card.getStatus());
        response.setBalance(card.getBalance());
        response.setExpirationDate(card.getExpirationDate());
        return response;
    }
}

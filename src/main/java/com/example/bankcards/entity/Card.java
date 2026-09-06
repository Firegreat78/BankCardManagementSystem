package com.example.bankcards.entity;

import com.example.bankcards.security.CardNumberCryptoConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@Entity
public class Card {

    @Id
    private String id;

    @NotBlank
    @Pattern(
            regexp = "^[0-9]{16}$",
            message = "Card number must be exactly 16 digits"
    )
    @Convert(converter = CardNumberCryptoConverter.class)
    @JsonSerialize(using = CardNumberMaskingSerializer.class)
    private String number;

    @JsonIgnore
    @Column(unique = true)
    private String numberHash;

    /** Searchable copy of the last four digits; see V2__add_card_last4.sql. */
    @JsonIgnore
    @Column(length = 4)
    private String last4;

    @NotBlank
    private String holderId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private CardStatus status = CardStatus.ACTIVE;

    @DecimalMin(
            value = "0.0",
            message = "Balance cannot be negative"
    )
    private BigDecimal balance;

    @NotNull
    private LocalDate expirationDate;
}
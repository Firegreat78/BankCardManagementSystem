package com.example.bankcards.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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
    private String number;

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
package com.example.bankcards.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class Card {
    @Id
    String id;

    @NotBlank
    @Pattern(regexp = "^[0-9]{16}$", message = "Card number must be exactly 16 digits")
    String number;

    @NotBlank
    String holderId;

    @DecimalMin(value = "0.0", message = "Balance cannot be negative")
    BigDecimal balance;
}
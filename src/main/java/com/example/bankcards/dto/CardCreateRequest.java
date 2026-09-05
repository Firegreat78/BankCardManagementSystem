package com.example.bankcards.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CardCreateRequest {

    @NotBlank
    @Pattern(
            regexp = "^[0-9]{16}$",
            message = "Card number must be exactly 16 digits"
    )
    private String number;

    @NotBlank
    private String holderId;

    @NotNull
    @DecimalMin(
            value = "0.0",
            message = "Balance cannot be negative"
    )
    private BigDecimal balance;

    @NotNull
    private LocalDate expirationDate;
}

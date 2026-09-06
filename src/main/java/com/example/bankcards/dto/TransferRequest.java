package com.example.bankcards.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {

    @NotBlank
    private String fromId;

    @NotBlank
    private String toId;

    /**
     * Balances are stored as NUMERIC(38,2), so an amount with more decimal
     * places would be rounded on write and could move a different sum than the
     * one the client asked for.
     */
    @NotNull
    @DecimalMin(value = "0", inclusive = false, message = "Amount must be positive")
    @Digits(integer = 36, fraction = 2, message = "Amount cannot have more than 2 decimal places")
    private BigDecimal amount;
}

package com.example.bankcards.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class User {
    private String id;

    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
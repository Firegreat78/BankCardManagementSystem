package com.example.bankcards.dto;

import com.example.bankcards.entity.Role;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRegisterRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    /** Optional; only administrators can reach this endpoint, so granting ADMIN here is deliberate. */
    private Role role = Role.USER;
}

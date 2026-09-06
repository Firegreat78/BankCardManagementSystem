package com.example.bankcards.dto;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import lombok.Data;

@Data
public class UserResponse {

    private String id;
    private String username;
    private Role role;

    public static UserResponse from(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        return response;
    }
}

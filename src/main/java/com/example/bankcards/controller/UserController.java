package com.example.bankcards.controller;

import com.example.bankcards.entity.User;
import jakarta.validation.Valid;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.example.bankcards.security.JwtUtil;
import com.example.bankcards.entity.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@RestController
@RequestMapping("/users")
public class UserController {

    private final List<User> users = new ArrayList<>();

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public User register(@RequestBody @Valid User user,
                         @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Check authentication
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }

        String token = authHeader.substring(7);
        Role role = jwtUtil.extractRole(token);

        // Only ADMIN can register new users
        if (role != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin can register users");
        }

        boolean exists = users.stream().anyMatch(u -> u.getUsername().equals(user.getUsername()));
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        user.setId(UUID.randomUUID().toString());
        users.add(user);
        return user;
    }
}
package com.example.bankcards.controller;

import com.example.bankcards.entity.User;
import com.example.bankcards.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.example.bankcards.security.JwtUtil;
import com.example.bankcards.entity.Role;
import org.springframework.web.bind.annotation.RequestHeader;


@RestController
@RequestMapping("/users")
public class UserController {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    public UserController(JwtUtil jwtUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

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

        return userService.register(user);
    }
}
package com.example.bankcards.controller;

import com.example.bankcards.config.AdminConfig;
import com.example.bankcards.entity.User;
import com.example.bankcards.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AdminConfig adminConfig;

    @Autowired
    private UserController userController;

    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (adminConfig.getUsername().equals(username) && adminConfig.getPassword().equals(password)) {
            return Map.of("token", jwtUtil.generateToken(username));
        }

        for (User user : userController.getUsers()) {
            if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                return Map.of("token", jwtUtil.generateToken(username));
            }
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
}
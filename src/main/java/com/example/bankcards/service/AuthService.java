package com.example.bankcards.service;

import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserJpaRepository;
import com.example.bankcards.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserJpaRepository userJpaRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserJpaRepository userJpaRepository,
            JwtUtil jwtUtil,
            PasswordEncoder passwordEncoder
    ) {
        this.userJpaRepository = userJpaRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Administrators are ordinary rows (see AdminBootstrap), so there is one
     * credential check for every account and the role always comes from the
     * database rather than from configuration.
     */
    public String login(String username, String password) {
        User user = userJpaRepository.findByUsername(username)
                .filter(candidate -> passwordEncoder.matches(password, candidate.getPassword()))
                .orElseThrow(() -> {
                    log.warn("Failed login attempt for username '{}'", username);
                    return new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "Invalid credentials"
                    );
                });

        return jwtUtil.generateToken(user.getUsername(), user.getRole());
    }
}

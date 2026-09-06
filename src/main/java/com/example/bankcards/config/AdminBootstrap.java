package com.example.bankcards.config;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

/**
 * Creates the configured administrator as an ordinary user row on first start,
 * so authentication has a single path and admins are managed like any other
 * user. The password is only read from configuration; it is stored hashed.
 */
@Configuration
public class AdminBootstrap {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    @Bean
    public ApplicationRunner seedAdmin(
            AdminConfig adminConfig,
            UserJpaRepository userJpaRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (userJpaRepository.existsByUsername(adminConfig.getUsername())) {
                return;
            }

            User admin = new User();
            admin.setId(UUID.randomUUID().toString());
            admin.setUsername(adminConfig.getUsername());
            admin.setPassword(passwordEncoder.encode(adminConfig.getPassword()));
            admin.setRole(Role.ADMIN);
            userJpaRepository.save(admin);

            log.info("Created initial administrator '{}'", adminConfig.getUsername());
        };
    }
}

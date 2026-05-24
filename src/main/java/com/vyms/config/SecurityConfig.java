package com.vyms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Security helper configuration for reusable security beans.
 *
 * In this project, it mainly exposes a BCrypt password encoder used when
 * creating users and validating login passwords.
 */
@Configuration
public class SecurityConfig {

    /**
     * Creates one BCrypt encoder bean managed by Spring.
     *
     * Why BCrypt: it hashes passwords with salt and work factor, making brute
     * force attacks harder than plain hashing methods.
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

package com.example.vault.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Separate configuration for H2 Console access
 * This runs with higher precedence than the main Vaadin security config
 */
@Configuration
@ConditionalOnProperty(name = "spring.h2.console.enabled", havingValue = "true")
public class H2ConsoleSecurityConfig {

    @Bean
    @Order(0) // Highest precedence - before any other security config
    public SecurityFilterChain h2ConsoleFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/h2-console/**")
            .authorizeHttpRequests(authz -> authz.anyRequest().permitAll())
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions().sameOrigin())
            .build();
    }
}

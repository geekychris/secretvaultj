package com.example.vault.config;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.annotation.Order;

/**
 * Simple Vaadin security configuration
 * Focus on getting the UI working first
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class VaadinSecurityConfig extends VaadinWebSecurity {

    private static final Logger logger = LoggerFactory.getLogger(VaadinSecurityConfig.class);
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Set LoginView as the login view
        setLoginView(http, com.example.vault.ui.views.LoginView.class);
        
        // Configure additional settings for Vaadin communication
        http
            .headers(headers -> headers
                .frameOptions().sameOrigin() // For H2 console compatibility
            );
        
        // Call super.configure to get all the Vaadin security defaults
        super.configure(http);
        
        // Configure form login with success URL
        http.formLogin(form -> form
            .defaultSuccessUrl("/secrets", true)
        );
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}

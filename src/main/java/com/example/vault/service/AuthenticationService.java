package com.example.vault.service;

import com.example.vault.entity.Identity;
import com.example.vault.entity.Policy;
import com.example.vault.repository.IdentityRepository;
import com.example.vault.security.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuthenticationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    
    @Autowired
    private IdentityRepository identityRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
    @Transactional
    public Optional<String> authenticate(String username, String password) {
        logger.info("Attempting authentication for user: {}", username);
        
        Optional<Identity> identityOpt = identityRepository.findByNameWithPolicies(username);
        
        if (identityOpt.isEmpty()) {
            logger.warn("Authentication failed: user not found - {}", username);
            return Optional.empty();
        }
        
        Identity identity = identityOpt.get();
        
        if (!identity.getEnabled()) {
            logger.warn("Authentication failed: user disabled - {}", username);
            return Optional.empty();
        }
        
        if (!passwordEncoder.matches(password, identity.getPasswordHash())) {
            logger.warn("Authentication failed: invalid password for user - {}", username);
            return Optional.empty();
        }
        
        // Update last login time
        identity.setLastLoginAt(LocalDateTime.now());
        identityRepository.save(identity);
        
        // Generate JWT token
        List<String> policies = identity.getPolicies().stream()
                .map(Policy::getName)
                .toList();
        
        String token = jwtTokenUtil.generateToken(
            username, 
            identity.getType().name(), 
            policies
        );
        
        logger.info("Authentication successful for user: {}", username);
        return Optional.of(token);
    }
    
    public boolean validateToken(String token) {
        try {
            String username = jwtTokenUtil.getUsernameFromToken(token);
            return jwtTokenUtil.validateToken(token, username);
        } catch (Exception e) {
            logger.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    public Optional<Identity> getIdentityFromToken(String token) {
        try {
            String username = jwtTokenUtil.getUsernameFromToken(token);
            if (jwtTokenUtil.validateToken(token, username)) {
                return identityRepository.findByNameWithPolicies(username);
            }
        } catch (Exception e) {
            logger.warn("Failed to extract identity from token: {}", e.getMessage());
        }
        return Optional.empty();
    }
    
    public List<String> getPoliciesFromToken(String token) {
        try {
            return jwtTokenUtil.getPoliciesFromToken(token);
        } catch (Exception e) {
            logger.warn("Failed to extract policies from token: {}", e.getMessage());
            return List.of();
        }
    }
}

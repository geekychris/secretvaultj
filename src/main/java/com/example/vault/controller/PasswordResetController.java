package com.example.vault.controller;

import com.example.vault.entity.Identity;
import com.example.vault.repository.IdentityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Temporary password reset utility for debugging authentication issues
 */
@RestController
@RequestMapping("/api/debug")
public class PasswordResetController {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetController.class);

    @Autowired
    private IdentityRepository identityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/reset-password/{username}/{newPassword}")
    @Transactional
    public String resetPassword(@PathVariable String username, @PathVariable String newPassword) {
        logger.info("Password reset requested for user: {}", username);
        
        Optional<Identity> identityOpt = identityRepository.findByName(username);
        if (identityOpt.isEmpty()) {
            return "User not found: " + username;
        }

        Identity identity = identityOpt.get();
        String oldHash = identity.getPasswordHash();
        String newHash = passwordEncoder.encode(newPassword);
        
        identity.setPasswordHash(newHash);
        identityRepository.save(identity);
        
        logger.info("Password reset completed for user: {}", username);
        logger.debug("Old hash: {}", oldHash);
        logger.debug("New hash: {}", newHash);
        
        return "Password reset successful for user: " + username;
    }

    @GetMapping("/check-password/{username}")
    public String checkPassword(@PathVariable String username) {
        Optional<Identity> identityOpt = identityRepository.findByName(username);
        if (identityOpt.isEmpty()) {
            return "User not found: " + username;
        }

        Identity identity = identityOpt.get();
        return "User: " + username + 
               "\nEnabled: " + identity.getEnabled() + 
               "\nPassword hash: " + identity.getPasswordHash() +
               "\nPolicies: " + identity.getPolicies().size();
    }
}

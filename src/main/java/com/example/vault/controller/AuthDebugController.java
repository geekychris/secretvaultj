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
 * Enhanced debugging controller for authentication issues
 */
@RestController
@RequestMapping("/api/auth-debug")
public class AuthDebugController {

    private static final Logger logger = LoggerFactory.getLogger(AuthDebugController.class);

    @Autowired
    private IdentityRepository identityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/compare-passwords/{username}")
    public String comparePasswords(@PathVariable String username) {
        StringBuilder result = new StringBuilder();
        
        Optional<Identity> identityOpt = identityRepository.findByName(username);
        if (identityOpt.isEmpty()) {
            return "User not found: " + username;
        }

        Identity identity = identityOpt.get();
        String storedHash = identity.getPasswordHash();
        
        result.append("User: ").append(username).append("\n");
        result.append("Stored hash: ").append(storedHash).append("\n");
        result.append("Hash length: ").append(storedHash.length()).append("\n");
        result.append("Hash starts with: ").append(storedHash.substring(0, Math.min(10, storedHash.length()))).append("\n");
        
        // Test different common passwords
        String[] testPasswords = {username, username + "1", "password", "admin123", "chris1"};
        
        for (String testPassword : testPasswords) {
            boolean matches = passwordEncoder.matches(testPassword, storedHash);
            result.append("Password '").append(testPassword).append("' matches: ").append(matches).append("\n");
        }
        
        // Generate a fresh hash with the expected password
        String expectedPassword = "chris1";
        String freshHash = passwordEncoder.encode(expectedPassword);
        result.append("\nFresh hash for '").append(expectedPassword).append("': ").append(freshHash).append("\n");
        result.append("Fresh hash matches expected: ").append(passwordEncoder.matches(expectedPassword, freshHash)).append("\n");
        
        return result.toString();
    }

    @PostMapping("/force-reset/{username}/{password}")
    @Transactional
    public String forceResetPassword(@PathVariable String username, @PathVariable String password) {
        Optional<Identity> identityOpt = identityRepository.findByName(username);
        if (identityOpt.isEmpty()) {
            return "User not found: " + username;
        }

        Identity identity = identityOpt.get();
        String oldHash = identity.getPasswordHash();
        
        // Force a completely fresh hash
        String newHash = passwordEncoder.encode(password);
        identity.setPasswordHash(newHash);
        identityRepository.saveAndFlush(identity); // Force immediate save
        
        // Verify the change took effect
        Identity refreshed = identityRepository.findByName(username).get();
        boolean verification = passwordEncoder.matches(password, refreshed.getPasswordHash());
        
        return "Password reset for " + username + "\n" +
               "Old hash: " + oldHash + "\n" +
               "New hash: " + newHash + "\n" +
               "Verification: " + verification + "\n" +
               "Database updated: " + !oldHash.equals(refreshed.getPasswordHash());
    }

    @GetMapping("/user-details/{username}")
    public String getUserDetails(@PathVariable String username) {
        Optional<Identity> identityOpt = identityRepository.findByNameWithPolicies(username);
        if (identityOpt.isEmpty()) {
            return "User not found: " + username;
        }

        Identity identity = identityOpt.get();
        StringBuilder result = new StringBuilder();
        
        result.append("User Details:\n");
        result.append("Name: ").append(identity.getName()).append("\n");
        result.append("Type: ").append(identity.getType()).append("\n");
        result.append("Enabled: ").append(identity.getEnabled()).append("\n");
        result.append("Policies: ").append(identity.getPolicies().size()).append("\n");
        result.append("Created: ").append(identity.getCreatedAt()).append("\n");
        result.append("Updated: ").append(identity.getUpdatedAt()).append("\n");
        result.append("Password hash length: ").append(identity.getPasswordHash().length()).append("\n");
        
        return result.toString();
    }
}

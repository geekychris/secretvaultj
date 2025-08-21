package com.example.vault.controller;

import com.example.vault.dto.AuthRequest;
import com.example.vault.service.AuthenticationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody AuthRequest request) {
        logger.info("Login attempt for user: {}", request.getUsername());
        
        Optional<String> tokenOpt = authenticationService.authenticate(
            request.getUsername(), 
            request.getPassword()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        
        if (tokenOpt.isPresent()) {
            response.put("success", true);
            response.put("message", "Authentication successful");
            response.put("token", tokenOpt.get());
            response.put("token_type", "Bearer");
            
            logger.info("Authentication successful for user: {}", request.getUsername());
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Authentication failed");
            response.put("error", "Invalid credentials");
            
            logger.warn("Authentication failed for user: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
    
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestHeader("Authorization") String authHeader) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.put("valid", false);
            response.put("message", "Invalid token format");
            return ResponseEntity.badRequest().body(response);
        }
        
        String token = authHeader.substring(7);
        boolean isValid = authenticationService.validateToken(token);
        
        response.put("valid", isValid);
        response.put("message", isValid ? "Token is valid" : "Token is invalid or expired");
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("authenticated", false);
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (authenticationService.validateToken(token)) {
                response.put("authenticated", true);
                response.put("policies", authenticationService.getPoliciesFromToken(token));
            }
        }
        
        return ResponseEntity.ok(response);
    }
}

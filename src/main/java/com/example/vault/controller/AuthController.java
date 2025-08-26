package com.example.vault.controller;

import com.example.vault.dto.AuthRequest;
import com.example.vault.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Authentication", description = "Authentication and token management endpoints")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @Operation(
            summary = "Authenticate user and get JWT token",
            description = "Authenticates a user with username and password, returns a JWT token for subsequent API calls"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Authentication successful",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "success": true,
                                      "message": "Authentication successful",
                                      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                      "token_type": "Bearer",
                                      "timestamp": "2024-01-15T10:30:00"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401", 
                    description = "Authentication failed",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "success": false,
                                      "message": "Authentication failed",
                                      "error": "Invalid credentials",
                                      "timestamp": "2024-01-15T10:30:00"
                                    }
                                    """
                            )
                    )
            )
    })
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
    
    @Operation(
            summary = "Validate JWT token",
            description = "Validates the provided JWT token and returns its validity status"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Token validation result",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "valid": true,
                                      "message": "Token is valid",
                                      "timestamp": "2024-01-15T10:30:00"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400", 
                    description = "Invalid token format",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "valid": false,
                                      "message": "Invalid token format",
                                      "timestamp": "2024-01-15T10:30:00"
                                    }
                                    """
                            )
                    )
            )
    })
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(
            @Parameter(description = "Bearer token in Authorization header", example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            @RequestHeader("Authorization") String authHeader) {
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
    
    @Operation(
            summary = "Get authentication status",
            description = "Returns the current authentication status and associated policies if authenticated"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Authentication status retrieved",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "Authenticated",
                                            value = """
                                            {
                                              "authenticated": true,
                                              "policies": ["admin", "secrets-read"],
                                              "timestamp": "2024-01-15T10:30:00"
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "Not Authenticated",
                                            value = """
                                            {
                                              "authenticated": false,
                                              "timestamp": "2024-01-15T10:30:00"
                                            }
                                            """
                                    )
                            }
                    )
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(
            @Parameter(description = "Optional Bearer token in Authorization header")
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
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

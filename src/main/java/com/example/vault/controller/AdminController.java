package com.example.vault.controller;

import com.example.vault.dto.ApiResponse;
import com.example.vault.dto.CreateUserRequest;
import com.example.vault.dto.UserResponse;
import com.example.vault.service.IdentityService;
import com.example.vault.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/v1/admin")
@Tag(name = "Administration", description = "Administrative endpoints for user and policy management")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    @Autowired
    private IdentityService identityService;
    
    @Autowired
    private PolicyService policyService;
    
    @Operation(
        summary = "Create new user account",
        description = "Creates a new user account with specified policies. Only accessible by administrators."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "User created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "success": true,
                      "message": "User created successfully",
                      "timestamp": "2024-01-15T10:30:00",
                      "data": {
                        "id": 123,
                        "username": "developer1",
                        "type": "USER",
                        "enabled": true,
                        "policies": ["developer"],
                        "createdAt": "2024-01-15T10:30:00"
                      }
                    }
                    """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "success": false,
                      "message": "Username 'developer1' already exists",
                      "error": "Validation failed",
                      "timestamp": "2024-01-15T10:30:00"
                    }
                    """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Access denied - admin privileges required"
        )
    })
    @PreAuthorize("hasAuthority('admin')")
    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        logger.info("Admin creating new user: {}", request.getUsername());
        
        try {
            UserResponse userResponse = identityService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", userResponse));
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to create user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to create user", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error creating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error", "An unexpected error occurred"));
        }
    }
    
    @Operation(
        summary = "Get user by username",
        description = "Retrieves user information by username. Only accessible by administrators."
    )
    @PreAuthorize("hasAuthority('admin')")
    @GetMapping("/users/{username}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
        @Parameter(description = "Username to retrieve", example = "developer1")
        @PathVariable String username) {
        
        logger.info("Admin retrieving user: {}", username);
        
        Optional<UserResponse> userOpt = identityService.getUserByUsername(username);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("User not found", "User '" + username + "' does not exist"));
        }
        
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", userOpt.get()));
    }
    
    @Operation(
        summary = "List all users",
        description = "Retrieves a list of all user accounts. Only accessible by administrators."
    )
    @PreAuthorize("hasAuthority('admin')")
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        logger.info("Admin retrieving all users");
        
        List<UserResponse> users = identityService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }
    
    @Operation(
        summary = "Update user account",
        description = "Updates an existing user account. Only accessible by administrators."
    )
    @PreAuthorize("hasAuthority('admin')")
    @PutMapping("/users/{username}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
        @Parameter(description = "Username to update", example = "developer1")
        @PathVariable String username,
        @Valid @RequestBody CreateUserRequest updateRequest) {
        
        logger.info("Admin updating user: {}", username);
        
        try {
            Optional<UserResponse> updatedUserOpt = identityService.updateUser(username, updateRequest);
            
            if (updatedUserOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("User not found", "User '" + username + "' does not exist"));
            }
            
            return ResponseEntity.ok(ApiResponse.success("User updated successfully", updatedUserOpt.get()));
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to update user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to update user", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error updating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error", "An unexpected error occurred"));
        }
    }
    
    @Operation(
        summary = "Delete user account",
        description = "Deletes a user account permanently. Only accessible by administrators."
    )
    @PreAuthorize("hasAuthority('admin')")
    @DeleteMapping("/users/{username}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
        @Parameter(description = "Username to delete", example = "developer1")
        @PathVariable String username) {
        
        logger.info("Admin deleting user: {}", username);
        
        try {
            boolean deleted = identityService.deleteUser(username);
            
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("User not found", "User '" + username + "' does not exist"));
            }
            
            return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to delete user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to delete user", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error deleting user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error", "An unexpected error occurred"));
        }
    }
    
    @Operation(
        summary = "Enable or disable user account",
        description = "Enables or disables a user account. Only accessible by administrators."
    )
    @PreAuthorize("hasAuthority('admin')")
    @PatchMapping("/users/{username}/status")
    public ResponseEntity<ApiResponse<Void>> toggleUserStatus(
        @Parameter(description = "Username to update", example = "developer1")
        @PathVariable String username,
        @Parameter(description = "Enable or disable the account", example = "true")
        @RequestParam boolean enabled) {
        
        logger.info("Admin {} user: {}", enabled ? "enabling" : "disabling", username);
        
        try {
            boolean updated = identityService.toggleUserStatus(username, enabled);
            
            if (!updated) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("User not found", "User '" + username + "' does not exist"));
            }
            
            String action = enabled ? "enabled" : "disabled";
            return ResponseEntity.ok(ApiResponse.success("User " + action + " successfully"));
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to toggle user status: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to update user status", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error toggling user status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error", "An unexpected error occurred"));
        }
    }
    
    @Operation(
        summary = "List available policies",
        description = "Retrieves a list of all available policies for user assignment. Only accessible by administrators."
    )
    @PreAuthorize("hasAuthority('admin')")
    @GetMapping("/policies")
    public ResponseEntity<ApiResponse<List<com.example.vault.entity.Policy>>> getAllPolicies() {
        logger.info("Admin retrieving all policies");
        
        List<com.example.vault.entity.Policy> policies = policyService.getAllPolicies();
        return ResponseEntity.ok(ApiResponse.success("Policies retrieved successfully", policies));
    }
}

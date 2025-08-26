package com.example.vault.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Request to create a new user account")
public class CreateUserRequest {
    
    @Schema(description = "Username for the new account", example = "developer1", required = true)
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;
    
    @Schema(description = "Password for the new account", example = "SecurePassword123!", required = true)
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;
    
    @Schema(description = "Identity type", example = "USER", allowableValues = {"USER", "SERVICE", "ADMIN"}, required = true)
    @NotNull(message = "Identity type is required")
    private String type;
    
    @Schema(description = "List of policy names to assign to the user", example = "[\"developer\", \"readonly\"]", required = true)
    @NotEmpty(message = "At least one policy must be assigned")
    private List<String> policies;
    
    @Schema(description = "Whether the account should be enabled", example = "true")
    private Boolean enabled = true;
    
    @Schema(description = "Optional description for the user account", example = "Frontend developer - Team Alpha")
    private String description;
    
    public CreateUserRequest() {}
    
    public CreateUserRequest(String username, String password, String type, List<String> policies) {
        this.username = username;
        this.password = password;
        this.type = type;
        this.policies = policies;
    }
    
    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public List<String> getPolicies() { return policies; }
    public void setPolicies(List<String> policies) { this.policies = policies; }
    
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

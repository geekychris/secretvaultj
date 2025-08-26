package com.example.vault.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "User account information response")
public class UserResponse {
    
    @Schema(description = "Unique user ID", example = "123")
    private Long id;
    
    @Schema(description = "Username", example = "developer1")
    private String username;
    
    @Schema(description = "Identity type", example = "USER")
    private String type;
    
    @Schema(description = "Whether the account is enabled", example = "true")
    private Boolean enabled;
    
    @Schema(description = "List of assigned policy names", example = "[\"developer\", \"readonly\"]")
    private List<String> policies;
    
    @Schema(description = "Optional user description", example = "Frontend developer - Team Alpha")
    private String description;
    
    @Schema(description = "When the account was created", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "When the account was last updated", example = "2024-01-15T14:20:00")
    private LocalDateTime updatedAt;
    
    @Schema(description = "Last login timestamp", example = "2024-01-15T09:15:00")
    private LocalDateTime lastLoginAt;
    
    public UserResponse() {}
    
    public UserResponse(Long id, String username, String type, Boolean enabled, 
                       List<String> policies, String description) {
        this.id = id;
        this.username = username;
        this.type = type;
        this.enabled = enabled;
        this.policies = policies;
        this.description = description;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    
    public List<String> getPolicies() { return policies; }
    public void setPolicies(List<String> policies) { this.policies = policies; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}

package com.example.vault.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Authentication request with username and password")
public class AuthRequest {
    
    @Schema(description = "Username for authentication", example = "admin", required = true)
    @NotBlank(message = "Username is required")
    private String username;
    
    @Schema(description = "Password for authentication", example = "secretpassword", required = true)
    @NotBlank(message = "Password is required")
    private String password;
    
    public AuthRequest() {}
    
    public AuthRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

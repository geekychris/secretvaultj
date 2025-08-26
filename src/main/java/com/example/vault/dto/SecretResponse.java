package com.example.vault.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Secret data response")
public class SecretResponse {
    
    @Schema(description = "The secret path", example = "app/config/database")
    private String path;
    
    @Schema(description = "The secret key", example = "password")
    private String key;
    
    @Schema(description = "The secret value", example = "mysecretpassword")
    private String value;
    
    @Schema(description = "Secret version number", example = "3")
    private Integer version;
    
    @Schema(description = "Additional metadata associated with the secret")
    private Map<String, Object> metadata;
    
    @Schema(description = "When the secret was created", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "When the secret was last updated", example = "2024-01-15T14:20:00")
    private LocalDateTime updatedAt;
    
    @Schema(description = "Username of the creator", example = "admin")
    private String createdBy;

    public SecretResponse() {}

    public SecretResponse(String path, String key, String value, Integer version) {
        this.path = path;
        this.key = key;
        this.value = value;
        this.version = version;
    }

    // Getters and setters
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}

package com.example.vault.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

@Schema(description = "Request to create or update a secret")
public class SecretRequest {
    
    @Schema(description = "The secret value to store", example = "mysecretpassword")
    @NotBlank(message = "Value is required")
    private String value;
    
    @Schema(description = "Optional metadata associated with the secret", example = "{\"environment\": \"production\", \"owner\": \"team-alpha\"}")
    private Map<String, Object> metadata;
    
    public SecretRequest() {}
    
    public SecretRequest(String value, Map<String, Object> metadata) {
        this.value = value;
        this.metadata = metadata;
    }
    
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}

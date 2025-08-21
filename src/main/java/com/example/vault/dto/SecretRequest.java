package com.example.vault.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public class SecretRequest {
    
    @NotBlank(message = "Value is required")
    private String value;
    
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

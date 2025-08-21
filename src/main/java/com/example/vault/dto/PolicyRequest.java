package com.example.vault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public class PolicyRequest {
    
    @NotBlank(message = "Policy name is required")
    private String name;
    
    private String description;
    
    @NotEmpty(message = "At least one rule is required")
    private Set<String> rules;
    
    public PolicyRequest() {}
    
    public PolicyRequest(String name, String description, Set<String> rules) {
        this.name = name;
        this.description = description;
        this.rules = rules;
    }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Set<String> getRules() { return rules; }
    public void setRules(Set<String> rules) { this.rules = rules; }
}

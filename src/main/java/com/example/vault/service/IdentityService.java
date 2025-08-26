package com.example.vault.service;

import com.example.vault.dto.CreateUserRequest;
import com.example.vault.dto.UserResponse;
import com.example.vault.entity.Identity;
import com.example.vault.entity.Policy;
import com.example.vault.repository.IdentityRepository;
import com.example.vault.repository.PolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class IdentityService {
    
    private static final Logger logger = LoggerFactory.getLogger(IdentityService.class);
    
    @Autowired
    private IdentityRepository identityRepository;
    
    @Autowired
    private PolicyRepository policyRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        logger.info("Creating new user: {}", request.getUsername());
        
        // Check if username already exists
        if (identityRepository.existsByName(request.getUsername())) {
            throw new IllegalArgumentException("Username '" + request.getUsername() + "' already exists");
        }
        
        // Validate identity type
        Identity.IdentityType identityType;
        try {
            identityType = Identity.IdentityType.valueOf(request.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid identity type: " + request.getType() + 
                ". Valid types are: USER, SERVICE, ADMIN");
        }
        
        // Validate and retrieve policies
        Set<Policy> policies = validateAndGetPolicies(request.getPolicies());
        
        // Create new identity
        Identity identity = new Identity(
            request.getUsername(),
            passwordEncoder.encode(request.getPassword()),
            identityType
        );
        
        identity.setEnabled(request.getEnabled());
        identity.setPolicies(policies);
        
        // Save identity
        Identity savedIdentity = identityRepository.save(identity);
        
        logger.info("Successfully created user: {} with ID: {}", savedIdentity.getName(), savedIdentity.getId());
        
        return convertToUserResponse(savedIdentity);
    }
    
    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserByUsername(String username) {
        Optional<Identity> identity = identityRepository.findByNameWithPolicies(username);
        return identity.map(this::convertToUserResponse);
    }
    
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        List<Identity> identities = identityRepository.findAll();
        return identities.stream()
            .map(this::convertToUserResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public Optional<UserResponse> updateUser(String username, CreateUserRequest updateRequest) {
        Optional<Identity> identityOpt = identityRepository.findByName(username);
        
        if (identityOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Identity identity = identityOpt.get();
        
        // Update password if provided
        if (updateRequest.getPassword() != null && !updateRequest.getPassword().isEmpty()) {
            identity.setPasswordHash(passwordEncoder.encode(updateRequest.getPassword()));
        }
        
        // Update identity type if provided
        if (updateRequest.getType() != null) {
            try {
                Identity.IdentityType identityType = Identity.IdentityType.valueOf(updateRequest.getType().toUpperCase());
                identity.setType(identityType);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid identity type: " + updateRequest.getType());
            }
        }
        
        // Update policies if provided
        if (updateRequest.getPolicies() != null) {
            Set<Policy> policies = validateAndGetPolicies(updateRequest.getPolicies());
            identity.setPolicies(policies);
        }
        
        // Update enabled status if provided
        if (updateRequest.getEnabled() != null) {
            identity.setEnabled(updateRequest.getEnabled());
        }
        
        Identity updatedIdentity = identityRepository.save(identity);
        logger.info("Updated user: {}", username);
        
        return Optional.of(convertToUserResponse(updatedIdentity));
    }
    
    @Transactional
    public boolean deleteUser(String username) {
        Optional<Identity> identityOpt = identityRepository.findByName(username);
        
        if (identityOpt.isEmpty()) {
            return false;
        }
        
        // Don't allow deletion of admin user
        Identity identity = identityOpt.get();
        if ("admin".equals(identity.getName())) {
            throw new IllegalArgumentException("Cannot delete the admin user");
        }
        
        identityRepository.delete(identity);
        logger.info("Deleted user: {}", username);
        return true;
    }
    
    @Transactional
    public boolean toggleUserStatus(String username, boolean enabled) {
        Optional<Identity> identityOpt = identityRepository.findByName(username);
        
        if (identityOpt.isEmpty()) {
            return false;
        }
        
        Identity identity = identityOpt.get();
        
        // Don't allow disabling admin user
        if ("admin".equals(identity.getName()) && !enabled) {
            throw new IllegalArgumentException("Cannot disable the admin user");
        }
        
        identity.setEnabled(enabled);
        identityRepository.save(identity);
        
        logger.info("{} user: {}", enabled ? "Enabled" : "Disabled", username);
        return true;
    }
    
    private Set<Policy> validateAndGetPolicies(List<String> policyNames) {
        Set<Policy> policies = new HashSet<>();
        
        for (String policyName : policyNames) {
            Optional<Policy> policyOpt = policyRepository.findByName(policyName);
            if (policyOpt.isEmpty()) {
                throw new IllegalArgumentException("Policy not found: " + policyName);
            }
            policies.add(policyOpt.get());
        }
        
        return policies;
    }
    
    private UserResponse convertToUserResponse(Identity identity) {
        List<String> policyNames = identity.getPolicies().stream()
            .map(Policy::getName)
            .collect(Collectors.toList());
        
        UserResponse response = new UserResponse(
            identity.getId(),
            identity.getName(),
            identity.getType().name(),
            identity.getEnabled(),
            policyNames,
            null // description field not in Identity entity yet
        );
        
        response.setCreatedAt(identity.getCreatedAt());
        response.setUpdatedAt(identity.getUpdatedAt());
        response.setLastLoginAt(identity.getLastLoginAt());
        
        return response;
    }
}

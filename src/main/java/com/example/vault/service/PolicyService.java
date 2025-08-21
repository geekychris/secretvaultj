package com.example.vault.service;

import com.example.vault.entity.Policy;
import com.example.vault.repository.PolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class PolicyService {
    
    private static final Logger logger = LoggerFactory.getLogger(PolicyService.class);
    
    @Autowired
    private PolicyRepository policyRepository;
    
    @Transactional
    public Policy createPolicy(String name, String description, Set<String> rules) {
        if (policyRepository.existsByName(name)) {
            throw new IllegalArgumentException("Policy with name '" + name + "' already exists");
        }
        
        Policy policy = new Policy(name, description);
        policy.setRules(rules);
        
        Policy savedPolicy = policyRepository.save(policy);
        logger.info("Created policy: {}", name);
        return savedPolicy;
    }
    
    @Transactional
    public Optional<Policy> updatePolicy(String name, String description, Set<String> rules) {
        Optional<Policy> policyOpt = policyRepository.findByName(name);
        
        if (policyOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Policy policy = policyOpt.get();
        if (description != null) {
            policy.setDescription(description);
        }
        if (rules != null) {
            policy.setRules(rules);
        }
        
        Policy updatedPolicy = policyRepository.save(policy);
        logger.info("Updated policy: {}", name);
        return Optional.of(updatedPolicy);
    }
    
    @Transactional
    public boolean deletePolicy(String name) {
        Optional<Policy> policyOpt = policyRepository.findByName(name);
        
        if (policyOpt.isEmpty()) {
            return false;
        }
        
        policyRepository.delete(policyOpt.get());
        logger.info("Deleted policy: {}", name);
        return true;
    }
    
    public Optional<Policy> getPolicy(String name) {
        return policyRepository.findByName(name);
    }
    
    public List<Policy> getAllPolicies() {
        return policyRepository.findAll();
    }
    
    /**
     * Check if the given policies allow access to the specified path and operation
     */
    public boolean hasAccess(List<String> policyNames, String path, String operation) {
        if (policyNames == null || policyNames.isEmpty()) {
            return false;
        }
        
        // Admin policy allows everything
        if (policyNames.contains("admin")) {
            return true;
        }
        
        for (String policyName : policyNames) {
            Optional<Policy> policyOpt = policyRepository.findByName(policyName);
            if (policyOpt.isPresent()) {
                Policy policy = policyOpt.get();
                if (evaluatePolicy(policy, path, operation)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private boolean evaluatePolicy(Policy policy, String path, String operation) {
        for (String rule : policy.getRules()) {
            if (matchesRule(rule, path, operation)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Evaluate if a rule matches the given path and operation.
     * Rule format: "operation:path_pattern"
     * Examples:
     * - "read:secret/*" allows read on any path under secret/
     * - "write:secret/myapp/*" allows write on paths under secret/myapp/
     * - "*:secret/public/*" allows any operation on paths under secret/public/
     */
    private boolean matchesRule(String rule, String path, String operation) {
        String[] parts = rule.split(":", 2);
        if (parts.length != 2) {
            logger.warn("Invalid policy rule format: {}", rule);
            return false;
        }
        
        String ruleOperation = parts[0].trim();
        String rulePathPattern = parts[1].trim();
        
        // Check operation match
        if (!ruleOperation.equals("*") && !ruleOperation.equalsIgnoreCase(operation)) {
            return false;
        }
        
        // Convert glob pattern to regex
        String regex = rulePathPattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".");
        
        Pattern pattern = Pattern.compile("^" + regex + "$");
        return pattern.matcher(path).matches();
    }
}

package com.example.vault.config;

import com.example.vault.entity.Identity;
import com.example.vault.entity.Policy;
import com.example.vault.repository.IdentityRepository;
import com.example.vault.repository.PolicyRepository;
import com.example.vault.service.SecretService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    
    @Autowired
    private IdentityRepository identityRepository;
    
    @Autowired
    private PolicyRepository policyRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private SecretService secretService;
    
    @Override
    public void run(String... args) throws Exception {
        // Create default policies
        if (policyRepository.count() == 0) {
            createDefaultPolicies();
        }
        
        // Create default admin user
        if (identityRepository.count() == 0) {
            createDefaultAdmin();
            
            // Create sample secrets after creating admin user
            createSampleSecrets();
        }
        
        logger.info("Data initialization completed");
    }
    
    private void createDefaultPolicies() {
        // Admin policy - full access
        Policy adminPolicy = new Policy("admin", "Full administrative access");
        adminPolicy.setRules(Set.of("*:*"));
        policyRepository.save(adminPolicy);
        
        // Read-only policy
        Policy readOnlyPolicy = new Policy("readonly", "Read-only access to all secrets");
        readOnlyPolicy.setRules(Set.of(
            "read:secret/*",
            "list:secret/*"
        ));
        policyRepository.save(readOnlyPolicy);
        
        // Developer policy
        Policy devPolicy = new Policy("developer", "Developer access to development secrets");
        devPolicy.setRules(Set.of(
            "create:secret/dev/*",
            "read:secret/dev/*",
            "update:secret/dev/*",
            "delete:secret/dev/*",
            "list:secret/dev/*",
            "read:secret/shared/*"
        ));
        policyRepository.save(devPolicy);
        
        logger.info("Created default policies: admin, readonly, developer");
    }
    
    private void createDefaultAdmin() {
        Policy adminPolicy = policyRepository.findByName("admin").orElseThrow();
        
        Identity admin = new Identity("admin", passwordEncoder.encode("admin123"), Identity.IdentityType.ADMIN);
        admin.setPolicies(Set.of(adminPolicy));
        identityRepository.save(admin);
        
        // Create a demo user
        Policy devPolicy = policyRepository.findByName("developer").orElseThrow();
        Identity demoUser = new Identity("demo", passwordEncoder.encode("demo123"), Identity.IdentityType.USER);
        demoUser.setPolicies(Set.of(devPolicy));
        identityRepository.save(demoUser);
        
        logger.info("Created default users: admin (admin123), demo (demo123)");
        logger.warn("WARNING: Change default passwords in production!");
    }
    
    private void createSampleSecrets() {
        try {
            Identity admin = identityRepository.findByName("admin").orElseThrow();
            java.util.List<String> adminPolicies = Arrays.asList("admin");
            
            // Database configuration secrets
            Map<String, Object> dbMetadata = new HashMap<>();
            dbMetadata.put("environment", "production");
            dbMetadata.put("service", "database");
            
            secretService.createSecret("app/config/database", "host", "localhost:5432", dbMetadata, admin, adminPolicies);
            secretService.createSecret("app/config/database", "username", "app_user", dbMetadata, admin, adminPolicies);
            secretService.createSecret("app/config/database", "password", "super_secure_password", dbMetadata, admin, adminPolicies);
            
            // API keys
            Map<String, Object> apiMetadata = new HashMap<>();
            apiMetadata.put("environment", "production");
            apiMetadata.put("service", "external-api");
            
            secretService.createSecret("app/api-keys", "stripe", "sk_live_12345abcdef", apiMetadata, admin, adminPolicies);
            secretService.createSecret("app/api-keys", "sendgrid", "SG.abc123.xyz789", apiMetadata, admin, adminPolicies);
            
            // Development secrets
            Map<String, Object> devMetadata = new HashMap<>();
            devMetadata.put("environment", "development");
            devMetadata.put("service", "dev-tools");
            
            secretService.createSecret("dev/config", "debug_token", "debug_12345", devMetadata, admin, adminPolicies);
            secretService.createSecret("dev/config", "test_api_key", "test_api_key_67890", devMetadata, admin, adminPolicies);
            
            // Shared secrets
            Map<String, Object> sharedMetadata = new HashMap<>();
            sharedMetadata.put("environment", "all");
            sharedMetadata.put("service", "shared");
            
            secretService.createSecret("shared/config", "app_secret", "shared_secret_key", sharedMetadata, admin, adminPolicies);
            secretService.createSecret("shared/certificates", "ca_cert", "-----BEGIN CERTIFICATE-----\nMII...\n-----END CERTIFICATE-----", sharedMetadata, admin, adminPolicies);
            
            logger.info("Created sample secrets for demonstration");
        } catch (Exception e) {
            logger.error("Failed to create sample secrets", e);
        }
    }
}

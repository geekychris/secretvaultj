package com.example.vault.config;

import com.example.vault.entity.Identity;
import com.example.vault.entity.Policy;
import com.example.vault.repository.IdentityRepository;
import com.example.vault.repository.PolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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
    
    @Override
    public void run(String... args) throws Exception {
        // Create default policies
        if (policyRepository.count() == 0) {
            createDefaultPolicies();
        }
        
        // Create default admin user
        if (identityRepository.count() == 0) {
            createDefaultAdmin();
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
}

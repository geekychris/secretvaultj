package com.example.vault.security;

import com.example.vault.entity.Identity;
import com.example.vault.repository.IdentityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class VaultUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(VaultUserDetailsService.class);
    
    @Autowired
    private IdentityRepository identityRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Attempting to load user: {}", username);
        
        Identity identity = identityRepository.findByNameWithPolicies(username)
                .orElseThrow(() -> {
                    logger.warn("User not found: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        logger.info("Successfully loaded user: {} with {} policies", 
                username, identity.getPolicies().size());

        // Check if user is enabled
        if (!identity.getEnabled()) {
            logger.warn("User account is disabled: {}", username);
            throw new UsernameNotFoundException("User account is disabled: " + username);
        }

        // Convert policies to Spring Security authorities
        var authorities = identity.getPolicies().stream()
                .map(policy -> new SimpleGrantedAuthority("ROLE_" + policy.getName().toUpperCase()))
                .collect(Collectors.toList());

        // Add default USER role
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        logger.debug("User {} has authorities: {}", username, 
                authorities.stream()
                    .map(auth -> auth.getAuthority())
                    .collect(Collectors.toList()));

        return org.springframework.security.core.userdetails.User.builder()
                .username(identity.getName())
                .password(identity.getPasswordHash())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!identity.getEnabled())
                .build();
    }
}

package com.example.vault.service;

import com.example.vault.entity.Identity;
import com.example.vault.entity.Secret;
import com.example.vault.repository.SecretRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SecretService {
    
    private static final Logger logger = LoggerFactory.getLogger(SecretService.class);
    
    @Autowired
    private SecretRepository secretRepository;
    
    @Autowired
    private EncryptionService encryptionService;
    
    @Autowired
    private PolicyService policyService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Transactional
    @CacheEvict(value = "secrets", allEntries = true)
    public Secret createSecret(String path, String key, String value, Map<String, Object> metadata, 
                             Identity createdBy, List<String> policies) {
        
        // Validate path
        validatePath(path);
        
        // Check if secret already exists
        if (secretRepository.existsByPathAndKeyAndDeletedFalse(path, key)) {
            throw new IllegalArgumentException("Secret already exists at path: " + path + "/" + key);
        }
        
        // Check access
        if (!policyService.hasAccess(policies, path + "/" + key, "create")) {
            throw new SecurityException("Access denied: insufficient permissions to create secret at " + path + "/" + key);
        }
        
        // Encrypt the value
        String encryptedValue = encryptionService.encrypt(value);
        
        // Create secret
        Secret secret = new Secret(path, key, encryptedValue, createdBy);
        secret.setMetadata(serializeMetadata(metadata));
        
        Secret savedSecret = secretRepository.save(secret);
        logger.info("Created secret at path: {}/{} by user: {}", path, key, createdBy.getName());
        
        return savedSecret;
    }
    
    @Transactional
    @CacheEvict(value = "secrets", allEntries = true)
    public Optional<Secret> updateSecret(String path, String key, String value, Map<String, Object> metadata,
                                       Identity updatedBy, List<String> policies) {
        
        validatePath(path);
        
        // Check access
        if (!policyService.hasAccess(policies, path + "/" + key, "update")) {
            throw new SecurityException("Access denied: insufficient permissions to update secret at " + path + "/" + key);
        }
        
        Optional<Secret> secretOpt = secretRepository.findByPathAndKeyAndDeletedFalse(path, key);
        if (secretOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Secret existingSecret = secretOpt.get();
        
        // Create new version
        Integer nextVersion = secretRepository.findMaxVersionByPathAndKey(path, key);
        nextVersion = (nextVersion == null) ? 1 : nextVersion + 1;
        
        Secret newSecret = new Secret(path, key, encryptionService.encrypt(value), existingSecret.getCreatedBy());
        newSecret.setVersion(nextVersion);
        newSecret.setUpdatedBy(updatedBy);
        newSecret.setMetadata(metadata != null ? serializeMetadata(metadata) : existingSecret.getMetadata());
        
        Secret savedSecret = secretRepository.save(newSecret);
        logger.info("Updated secret at path: {}/{} to version {} by user: {}", path, key, nextVersion, updatedBy.getName());
        
        return Optional.of(savedSecret);
    }
    
    @Cacheable(value = "secrets", key = "#path + '/' + #key")
    public Optional<Map<String, Object>> getSecret(String path, String key, List<String> policies) {
        return getSecret(path, key, null, policies);
    }
    
    @Cacheable(value = "secrets", key = "#path + '/' + #key + '/' + #version")
    public Optional<Map<String, Object>> getSecret(String path, String key, Integer version, List<String> policies) {
        validatePath(path);
        
        // Check access
        if (!policyService.hasAccess(policies, path + "/" + key, "read")) {
            throw new SecurityException("Access denied: insufficient permissions to read secret at " + path + "/" + key);
        }
        
        Optional<Secret> secretOpt;
        if (version != null) {
            secretOpt = secretRepository.findByPathAndKeyAndVersion(path, key, version);
        } else {
            secretOpt = secretRepository.findByPathAndKeyAndDeletedFalse(path, key);
        }
        
        if (secretOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Secret secret = secretOpt.get();
        String decryptedValue = encryptionService.decrypt(secret.getEncryptedValue());
        
        Map<String, Object> result = new HashMap<>();
        result.put("value", decryptedValue);
        result.put("version", secret.getVersion());
        result.put("created_at", secret.getCreatedAt());
        result.put("updated_at", secret.getUpdatedAt());
        result.put("metadata", deserializeMetadata(secret.getMetadata()));
        
        return Optional.of(result);
    }
    
    @Transactional
    @CacheEvict(value = "secrets", allEntries = true)
    public boolean deleteSecret(String path, String key, List<String> policies) {
        validatePath(path);
        
        // Check access
        if (!policyService.hasAccess(policies, path + "/" + key, "delete")) {
            throw new SecurityException("Access denied: insufficient permissions to delete secret at " + path + "/" + key);
        }
        
        // Check if any non-deleted version exists
        Optional<Secret> secretOpt = secretRepository.findByPathAndKeyAndDeletedFalse(path, key);
        if (secretOpt.isEmpty()) {
            return false;
        }
        
        // Mark all versions of this secret as deleted
        List<Secret> allVersions = secretRepository.findAllByPathAndKey(path, key);
        LocalDateTime deletionTime = LocalDateTime.now();
        
        for (Secret secret : allVersions) {
            if (!secret.getDeleted()) {
                secret.setDeleted(true);
                secret.setDeletedAt(deletionTime);
            }
        }
        
        secretRepository.saveAll(allVersions);
        
        logger.info("Deleted secret at path: {}/{}", path, key);
        return true;
    }
    
    public List<String> listSecrets(String path, List<String> policies) {
        return listSecrets(path, false, policies);
    }
    
    public List<String> listSecrets(String path, boolean recursive, List<String> policies) {
        validatePath(path);
        
        // Check access for list operation
        if (!policyService.hasAccess(policies, path + "/*", "list")) {
            throw new SecurityException("Access denied: insufficient permissions to list secrets at " + path);
        }
        
        List<Secret> secrets;
        if (recursive) {
            secrets = secretRepository.findByPathPrefixAndDeletedFalse(path + "%");
        } else {
            secrets = secretRepository.findByPathAndDeletedFalse(path);
        }
        
        return secrets.stream()
                .map(secret -> secret.getPath() + "/" + secret.getKey())
                .sorted()
                .collect(Collectors.toList());
    }
    
    public List<String> listPaths(String pathPrefix, List<String> policies) {
        validatePath(pathPrefix);
        
        // Check access
        if (!policyService.hasAccess(policies, pathPrefix + "/*", "list")) {
            throw new SecurityException("Access denied: insufficient permissions to list paths under " + pathPrefix);
        }
        
        List<String> paths = secretRepository.findPathsByPrefix(pathPrefix + "%");
        return paths.stream()
                .sorted()
                .collect(Collectors.toList());
    }
    
    /**
     * List all versions of a specific secret
     */
    public List<Map<String, Object>> listSecretVersions(String path, String key, List<String> policies) {
        validatePath(path);
        
        // Check access
        if (!policyService.hasAccess(policies, path + "/" + key, "read")) {
            throw new SecurityException("Access denied: insufficient permissions to read secret versions at " + path + "/" + key);
        }
        
        List<Secret> versions = secretRepository.findAllVersionsByPathAndKey(path, key);
        
        return versions.stream()
                .map(secret -> {
                    Map<String, Object> versionInfo = new HashMap<>();
                    versionInfo.put("version", secret.getVersion());
                    versionInfo.put("created_at", secret.getCreatedAt());
                    versionInfo.put("updated_at", secret.getUpdatedAt());
                    versionInfo.put("created_by", secret.getCreatedBy() != null ? secret.getCreatedBy().getName() : null);
                    versionInfo.put("updated_by", secret.getUpdatedBy() != null ? secret.getUpdatedBy().getName() : null);
                    versionInfo.put("metadata", deserializeMetadata(secret.getMetadata()));
                    versionInfo.put("deleted", secret.getDeleted());
                    versionInfo.put("deleted_at", secret.getDeletedAt());
                    return versionInfo;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Get version count for a specific secret
     */
    public Long getSecretVersionCount(String path, String key, List<String> policies) {
        validatePath(path);
        
        // Check access
        if (!policyService.hasAccess(policies, path + "/" + key, "read")) {
            throw new SecurityException("Access denied: insufficient permissions to read secret at " + path + "/" + key);
        }
        
        return secretRepository.countVersionsByPathAndKey(path, key);
    }
    
    /**
     * Get version range information for a secret
     */
    public Map<String, Object> getSecretVersionInfo(String path, String key, List<String> policies) {
        validatePath(path);
        
        // Check access
        if (!policyService.hasAccess(policies, path + "/" + key, "read")) {
            throw new SecurityException("Access denied: insufficient permissions to read secret at " + path + "/" + key);
        }
        
        Long versionCount = secretRepository.countVersionsByPathAndKey(path, key);
        Integer minVersion = secretRepository.findMinVersionByPathAndKey(path, key);
        Integer maxVersion = secretRepository.findMaxVersionByPathAndKey(path, key);
        
        Map<String, Object> versionInfo = new HashMap<>();
        versionInfo.put("total_versions", versionCount);
        versionInfo.put("earliest_version", minVersion);
        versionInfo.put("latest_version", maxVersion);
        versionInfo.put("path", path + "/" + key);
        
        return versionInfo;
    }
    
    /**
     * Get a range of versions for a secret
     */
    public List<Map<String, Object>> getSecretVersionRange(String path, String key, Integer startVersion, 
                                                          Integer endVersion, List<String> policies) {
        validatePath(path);
        
        // Check access
        if (!policyService.hasAccess(policies, path + "/" + key, "read")) {
            throw new SecurityException("Access denied: insufficient permissions to read secret at " + path + "/" + key);
        }
        
        // Validate version range
        if (startVersion != null && endVersion != null && startVersion > endVersion) {
            throw new IllegalArgumentException("Start version cannot be greater than end version");
        }
        
        // Use repository method for range query
        List<Secret> versions = secretRepository.findVersionRangeByPathAndKey(path, key, startVersion, endVersion);
        
        return versions.stream()
                .map(secret -> {
                    String decryptedValue = encryptionService.decrypt(secret.getEncryptedValue());
                    Map<String, Object> result = new HashMap<>();
                    result.put("value", decryptedValue);
                    result.put("version", secret.getVersion());
                    result.put("created_at", secret.getCreatedAt());
                    result.put("updated_at", secret.getUpdatedAt());
                    result.put("created_by", secret.getCreatedBy() != null ? secret.getCreatedBy().getName() : null);
                    result.put("updated_by", secret.getUpdatedBy() != null ? secret.getUpdatedBy().getName() : null);
                    result.put("metadata", deserializeMetadata(secret.getMetadata()));
                    return result;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Delete a specific version of a secret
     */
    @Transactional
    @CacheEvict(value = "secrets", allEntries = true)
    public boolean deleteSecretVersion(String path, String key, Integer version, List<String> policies) {
        validatePath(path);
        
        // Check access
        if (!policyService.hasAccess(policies, path + "/" + key, "delete")) {
            throw new SecurityException("Access denied: insufficient permissions to delete secret at " + path + "/" + key);
        }
        
        Optional<Secret> secretOpt = secretRepository.findByPathAndKeyAndVersion(path, key, version);
        if (secretOpt.isEmpty() || secretOpt.get().getDeleted()) {
            return false;
        }
        
        Secret secret = secretOpt.get();
        secret.setDeleted(true);
        secret.setDeletedAt(LocalDateTime.now());
        secretRepository.save(secret);
        
        logger.info("Deleted secret version at path: {}/{} version: {}", path, key, version);
        return true;
    }
    
    /**
     * Restore a deleted version of a secret
     */
    @Transactional
    @CacheEvict(value = "secrets", allEntries = true)
    public boolean restoreSecretVersion(String path, String key, Integer version, List<String> policies) {
        validatePath(path);
        
        // Check access (using update permission for restoration)
        if (!policyService.hasAccess(policies, path + "/" + key, "update")) {
            throw new SecurityException("Access denied: insufficient permissions to restore secret at " + path + "/" + key);
        }
        
        Optional<Secret> secretOpt = secretRepository.findByPathAndKeyAndVersionIncludingDeleted(path, key, version);
        if (secretOpt.isEmpty() || !secretOpt.get().getDeleted()) {
            return false;
        }
        
        Secret secret = secretOpt.get();
        secret.setDeleted(false);
        secret.setDeletedAt(null);
        secretRepository.save(secret);
        
        logger.info("Restored secret version at path: {}/{} version: {}", path, key, version);
        return true;
    }
    
    private void validatePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }
        
        if (path.contains("..") || path.contains("//")) {
            throw new IllegalArgumentException("Invalid path: contains illegal characters");
        }
        
        if (path.startsWith("/")) {
            throw new IllegalArgumentException("Path should not start with /");
        }
        
        if (path.endsWith("/")) {
            throw new IllegalArgumentException("Path should not end with /");
        }
    }
    
    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            logger.warn("Failed to serialize metadata", e);
            return "{}";
        }
    }
    
    private Map<String, Object> deserializeMetadata(String metadata) {
        if (metadata == null || metadata.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            return objectMapper.readValue(metadata, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            logger.warn("Failed to deserialize metadata: {}", metadata, e);
            return new HashMap<>();
        }
    }
}

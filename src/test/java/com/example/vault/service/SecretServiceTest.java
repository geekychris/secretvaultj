package com.example.vault.service;

import com.example.vault.entity.Identity;
import com.example.vault.entity.Secret;
import com.example.vault.repository.SecretRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecretServiceTest {
    
    @Mock
    private SecretRepository secretRepository;
    
    @Mock
    private EncryptionService encryptionService;
    
    @Mock
    private PolicyService policyService;
    
    @InjectMocks
    private SecretService secretService;
    
    private Identity testIdentity;
    private List<String> testPolicies;
    
    @BeforeEach
    void setUp() {
        testIdentity = new Identity("testuser", "hashedpassword", Identity.IdentityType.USER);
        testIdentity.setId(1L);
        testPolicies = List.of("read-policy", "write-policy");
    }
    
    @Test
    void createSecret_Success() {
        // Arrange
        String path = "secret/app";
        String key = "database-password";
        String value = "mysecretpassword";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("environment", "production");
        
        when(secretRepository.existsByPathAndKeyAndDeletedFalse(path, key)).thenReturn(false);
        when(policyService.hasAccess(testPolicies, path + "/" + key, "create")).thenReturn(true);
        when(encryptionService.encrypt(value)).thenReturn("encrypted_value");
        
        Secret savedSecret = new Secret(path, key, "encrypted_value", testIdentity);
        when(secretRepository.save(any(Secret.class))).thenReturn(savedSecret);
        
        // Act
        Secret result = secretService.createSecret(path, key, value, metadata, testIdentity, testPolicies);
        
        // Assert
        assertNotNull(result);
        assertEquals(path, result.getPath());
        assertEquals(key, result.getKey());
        assertEquals("encrypted_value", result.getEncryptedValue());
        
        verify(secretRepository).existsByPathAndKeyAndDeletedFalse(path, key);
        verify(policyService).hasAccess(testPolicies, path + "/" + key, "create");
        verify(encryptionService).encrypt(value);
        verify(secretRepository).save(any(Secret.class));
    }
    
    @Test
    void createSecret_AlreadyExists_ThrowsException() {
        // Arrange
        String path = "secret/app";
        String key = "database-password";
        String value = "mysecretpassword";
        
        when(secretRepository.existsByPathAndKeyAndDeletedFalse(path, key)).thenReturn(true);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            secretService.createSecret(path, key, value, null, testIdentity, testPolicies));
        
        verify(secretRepository).existsByPathAndKeyAndDeletedFalse(path, key);
        verifyNoInteractions(encryptionService);
        verify(secretRepository, never()).save(any());
    }
    
    @Test
    void createSecret_AccessDenied_ThrowsSecurityException() {
        // Arrange
        String path = "secret/app";
        String key = "database-password";
        String value = "mysecretpassword";
        
        when(secretRepository.existsByPathAndKeyAndDeletedFalse(path, key)).thenReturn(false);
        when(policyService.hasAccess(testPolicies, path + "/" + key, "create")).thenReturn(false);
        
        // Act & Assert
        assertThrows(SecurityException.class, () -> 
            secretService.createSecret(path, key, value, null, testIdentity, testPolicies));
        
        verify(policyService).hasAccess(testPolicies, path + "/" + key, "create");
        verifyNoInteractions(encryptionService);
        verify(secretRepository, never()).save(any());
    }
    
    @Test
    void getSecret_Success() {
        // Arrange
        String path = "secret/app";
        String key = "database-password";
        String encryptedValue = "encrypted_value";
        String decryptedValue = "mysecretpassword";
        
        Secret secret = new Secret(path, key, encryptedValue, testIdentity);
        secret.setVersion(1);
        
        when(policyService.hasAccess(testPolicies, path + "/" + key, "read")).thenReturn(true);
        when(secretRepository.findByPathAndKeyAndDeletedFalse(path, key)).thenReturn(Optional.of(secret));
        when(encryptionService.decrypt(encryptedValue)).thenReturn(decryptedValue);
        
        // Act
        Optional<Map<String, Object>> result = secretService.getSecret(path, key, testPolicies);
        
        // Assert
        assertTrue(result.isPresent());
        Map<String, Object> secretData = result.get();
        assertEquals(decryptedValue, secretData.get("value"));
        assertEquals(1, secretData.get("version"));
        
        verify(policyService).hasAccess(testPolicies, path + "/" + key, "read");
        verify(secretRepository).findByPathAndKeyAndDeletedFalse(path, key);
        verify(encryptionService).decrypt(encryptedValue);
    }
    
    @Test
    void getSecret_NotFound_ReturnsEmpty() {
        // Arrange
        String path = "secret/app";
        String key = "nonexistent";
        
        when(policyService.hasAccess(testPolicies, path + "/" + key, "read")).thenReturn(true);
        when(secretRepository.findByPathAndKeyAndDeletedFalse(path, key)).thenReturn(Optional.empty());
        
        // Act
        Optional<Map<String, Object>> result = secretService.getSecret(path, key, testPolicies);
        
        // Assert
        assertTrue(result.isEmpty());
        
        verify(policyService).hasAccess(testPolicies, path + "/" + key, "read");
        verify(secretRepository).findByPathAndKeyAndDeletedFalse(path, key);
        verifyNoInteractions(encryptionService);
    }
    
    @Test
    void getSecret_AccessDenied_ThrowsSecurityException() {
        // Arrange
        String path = "secret/app";
        String key = "database-password";
        
        when(policyService.hasAccess(testPolicies, path + "/" + key, "read")).thenReturn(false);
        
        // Act & Assert
        assertThrows(SecurityException.class, () -> 
            secretService.getSecret(path, key, testPolicies));
        
        verify(policyService).hasAccess(testPolicies, path + "/" + key, "read");
        verifyNoInteractions(secretRepository);
        verifyNoInteractions(encryptionService);
    }
    
    @Test
    void deleteSecret_Success() {
        // Arrange
        String path = "secret/app";
        String key = "database-password";
        
        Secret secret1 = new Secret(path, key, "encrypted_value_v1", testIdentity);
        secret1.setVersion(1);
        Secret secret2 = new Secret(path, key, "encrypted_value_v2", testIdentity);
        secret2.setVersion(2);
        
        List<Secret> allVersions = Arrays.asList(secret1, secret2);
        
        when(policyService.hasAccess(testPolicies, path + "/" + key, "delete")).thenReturn(true);
        when(secretRepository.findByPathAndKeyAndDeletedFalse(path, key)).thenReturn(Optional.of(secret2));
        when(secretRepository.findAllByPathAndKey(path, key)).thenReturn(allVersions);
        when(secretRepository.saveAll(any(List.class))).thenReturn(allVersions);
        
        // Act
        boolean result = secretService.deleteSecret(path, key, testPolicies);
        
        // Assert
        assertTrue(result);
        assertTrue(secret1.getDeleted());
        assertTrue(secret2.getDeleted());
        assertNotNull(secret1.getDeletedAt());
        assertNotNull(secret2.getDeletedAt());
        
        verify(policyService).hasAccess(testPolicies, path + "/" + key, "delete");
        verify(secretRepository).findByPathAndKeyAndDeletedFalse(path, key);
        verify(secretRepository).findAllByPathAndKey(path, key);
        verify(secretRepository).saveAll(allVersions);
    }
    
    @Test
    void validatePath_InvalidPath_ThrowsException() {
        // Test null path
        assertThrows(IllegalArgumentException.class, () -> 
            secretService.createSecret(null, "key", "value", null, testIdentity, testPolicies));
        
        // Test empty path
        assertThrows(IllegalArgumentException.class, () -> 
            secretService.createSecret("", "key", "value", null, testIdentity, testPolicies));
        
        // Test path with ".."
        assertThrows(IllegalArgumentException.class, () -> 
            secretService.createSecret("secret/../admin", "key", "value", null, testIdentity, testPolicies));
        
        // Test path starting with "/"
        assertThrows(IllegalArgumentException.class, () -> 
            secretService.createSecret("/secret/app", "key", "value", null, testIdentity, testPolicies));
        
        // Test path ending with "/"
        assertThrows(IllegalArgumentException.class, () -> 
            secretService.createSecret("secret/app/", "key", "value", null, testIdentity, testPolicies));
    }
    
    // ============= VERSIONING TESTS =============
    
    @Test
    void updateSecret_CreatesNewVersion() {
        // Arrange
        String path = "secret/app";
        String key = "database-password";
        String originalValue = "original-password";
        String updatedValue = "updated-password";
        
        Secret existingSecret = new Secret(path, key, "encrypted_original", testIdentity);
        existingSecret.setVersion(1);
        
        when(policyService.hasAccess(testPolicies, path + "/" + key, "update")).thenReturn(true);
        when(secretRepository.findByPathAndKeyAndDeletedFalse(path, key)).thenReturn(Optional.of(existingSecret));
        when(secretRepository.findMaxVersionByPathAndKey(path, key)).thenReturn(1);
        when(encryptionService.encrypt(updatedValue)).thenReturn("encrypted_updated");
        
        Secret newSecret = new Secret(path, key, "encrypted_updated", testIdentity);
        newSecret.setVersion(2);
        when(secretRepository.save(any(Secret.class))).thenReturn(newSecret);
        
        // Act
        Optional<Secret> result = secretService.updateSecret(path, key, updatedValue, null, testIdentity, testPolicies);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(2, result.get().getVersion());
        
        verify(secretRepository).findMaxVersionByPathAndKey(path, key);
        verify(secretRepository).save(argThat(secret -> secret.getVersion().equals(2)));
    }
    
    @Test
    void getSecret_WithSpecificVersion_ReturnsCorrectVersion() {
        // Arrange
        String path = "secret/app";
        String key = "database-password";
        Integer version = 2;
        String encryptedValue = "encrypted_value_v2";
        String decryptedValue = "decrypted_value_v2";
        
        Secret secret = new Secret(path, key, encryptedValue, testIdentity);
        secret.setVersion(version);
        
        when(policyService.hasAccess(testPolicies, path + "/" + key, "read")).thenReturn(true);
        when(secretRepository.findByPathAndKeyAndVersion(path, key, version)).thenReturn(Optional.of(secret));
        when(encryptionService.decrypt(encryptedValue)).thenReturn(decryptedValue);
        
        // Act
        Optional<Map<String, Object>> result = secretService.getSecret(path, key, version, testPolicies);
        
        // Assert
        assertTrue(result.isPresent());
        Map<String, Object> secretData = result.get();
        assertEquals(decryptedValue, secretData.get("value"));
        assertEquals(version, secretData.get("version"));
        
        verify(secretRepository).findByPathAndKeyAndVersion(path, key, version);
    }
    
    @Test
    void listSecretVersions_ReturnsAllVersions() {
        // Arrange
        String path = "secret/app";
        String key = "database-password";
        
        Secret version1 = new Secret(path, key, "encrypted_v1", testIdentity);
        version1.setVersion(1);
        Secret version2 = new Secret(path, key, "encrypted_v2", testIdentity);
        version2.setVersion(2);
        
        List<Secret> versions = Arrays.asList(version2, version1); // Ordered by version DESC
        
        when(policyService.hasAccess(testPolicies, path + "/" + key, "read")).thenReturn(true);
        when(secretRepository.findAllVersionsByPathAndKey(path, key)).thenReturn(versions);
        
        // Act
        List<Map<String, Object>> result = secretService.listSecretVersions(path, key, testPolicies);
        
        // Assert
        assertEquals(2, result.size());
        assertEquals(2, result.get(0).get("version")); // First should be version 2
        assertEquals(1, result.get(1).get("version")); // Second should be version 1
        
        verify(secretRepository).findAllVersionsByPathAndKey(path, key);
    }
    
    @Test
    void getSecretVersionCount_ReturnsCorrectCount() {
        // Arrange
        String path = "secret/app";
        String key = "database-password";
        Long expectedCount = 3L;
        
        when(policyService.hasAccess(testPolicies, path + "/" + key, "read")).thenReturn(true);
        when(secretRepository.countVersionsByPathAndKey(path, key)).thenReturn(expectedCount);
        
        // Act
        Long result = secretService.getSecretVersionCount(path, key, testPolicies);
        
        // Assert
        assertEquals(expectedCount, result);
        
        verify(secretRepository).countVersionsByPathAndKey(path, key);
    }
    
    @Test
    void getSecretVersionInfo_ReturnsVersionSummary() {
        // Arrange
        String path = "secret/app";
        String key = "database-password";
        
        when(policyService.hasAccess(testPolicies, path + "/" + key, "read")).thenReturn(true);
        when(secretRepository.countVersionsByPathAndKey(path, key)).thenReturn(3L);
        when(secretRepository.findMinVersionByPathAndKey(path, key)).thenReturn(1);
        when(secretRepository.findMaxVersionByPathAndKey(path, key)).thenReturn(3);
        
        // Act
        Map<String, Object> result = secretService.getSecretVersionInfo(path, key, testPolicies);
        
        // Assert
        assertEquals(3L, result.get("total_versions"));
        assertEquals(1, result.get("earliest_version"));
        assertEquals(3, result.get("latest_version"));
        assertEquals(path + "/" + key, result.get("path"));
    }
    
    @Test
    void getSecretVersionRange_ReturnsCorrectRange() {
        // Arrange
        String path = "secret/app";
        String key = "database-password";
        Integer startVersion = 2;
        Integer endVersion = 3;
        
        Secret version2 = new Secret(path, key, "encrypted_v2", testIdentity);
        version2.setVersion(2);
        Secret version3 = new Secret(path, key, "encrypted_v3", testIdentity);
        version3.setVersion(3);
        
        List<Secret> versions = Arrays.asList(version3, version2);
        
        when(policyService.hasAccess(testPolicies, path + "/" + key, "read")).thenReturn(true);
        when(secretRepository.findVersionRangeByPathAndKey(path, key, startVersion, endVersion)).thenReturn(versions);
        when(encryptionService.decrypt("encrypted_v2")).thenReturn("decrypted_v2");
        when(encryptionService.decrypt("encrypted_v3")).thenReturn("decrypted_v3");
        
        // Act
        List<Map<String, Object>> result = secretService.getSecretVersionRange(path, key, startVersion, endVersion, testPolicies);
        
        // Assert
        assertEquals(2, result.size());
        assertEquals(3, result.get(0).get("version"));
        assertEquals("decrypted_v3", result.get(0).get("value"));
        assertEquals(2, result.get(1).get("version"));
        assertEquals("decrypted_v2", result.get(1).get("value"));
    }
    
    @Test
    void getSecretVersionRange_InvalidRange_ThrowsException() {
        // Arrange
        String path = "secret/app";
        String key = "database-password";
        Integer startVersion = 3;
        Integer endVersion = 1; // Invalid: start > end
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            secretService.getSecretVersionRange(path, key, startVersion, endVersion, testPolicies));
    }
    
    @Test
    void deleteSecretVersion_Success() {
        // Arrange
        String path = "secret/app";
        String key = "database-password";
        Integer version = 2;
        
        Secret secret = new Secret(path, key, "encrypted_value", testIdentity);
        secret.setVersion(version);
        secret.setDeleted(false);
        
        when(policyService.hasAccess(testPolicies, path + "/" + key, "delete")).thenReturn(true);
        when(secretRepository.findByPathAndKeyAndVersion(path, key, version)).thenReturn(Optional.of(secret));
        when(secretRepository.save(any(Secret.class))).thenReturn(secret);
        
        // Act
        boolean result = secretService.deleteSecretVersion(path, key, version, testPolicies);
        
        // Assert
        assertTrue(result);
        assertTrue(secret.getDeleted());
        assertNotNull(secret.getDeletedAt());
        
        verify(secretRepository).save(secret);
    }
    
    @Test
    void deleteSecretVersion_AlreadyDeleted_ReturnsFalse() {
        // Arrange
        String path = "secret/app";
        String key = "database-password";
        Integer version = 2;
        
        Secret secret = new Secret(path, key, "encrypted_value", testIdentity);
        secret.setVersion(version);
        secret.setDeleted(true); // Already deleted
        
        when(policyService.hasAccess(testPolicies, path + "/" + key, "delete")).thenReturn(true);
        when(secretRepository.findByPathAndKeyAndVersion(path, key, version)).thenReturn(Optional.of(secret));
        
        // Act
        boolean result = secretService.deleteSecretVersion(path, key, version, testPolicies);
        
        // Assert
        assertFalse(result);
        
        verify(secretRepository, never()).save(any());
    }
    
    @Test
    void restoreSecretVersion_Success() {
        // Arrange
        String path = "secret/app";
        String key = "database-password";
        Integer version = 2;
        
        Secret secret = new Secret(path, key, "encrypted_value", testIdentity);
        secret.setVersion(version);
        secret.setDeleted(true);
        
        when(policyService.hasAccess(testPolicies, path + "/" + key, "update")).thenReturn(true);
        when(secretRepository.findByPathAndKeyAndVersion(path, key, version)).thenReturn(Optional.of(secret));
        when(secretRepository.save(any(Secret.class))).thenReturn(secret);
        
        // Act
        boolean result = secretService.restoreSecretVersion(path, key, version, testPolicies);
        
        // Assert
        assertTrue(result);
        assertFalse(secret.getDeleted());
        assertNull(secret.getDeletedAt());
        
        verify(secretRepository).save(secret);
    }
    
    @Test
    void restoreSecretVersion_NotDeleted_ReturnsFalse() {
        // Arrange
        String path = "secret/app";
        String key = "database-password";
        Integer version = 2;
        
        Secret secret = new Secret(path, key, "encrypted_value", testIdentity);
        secret.setVersion(version);
        secret.setDeleted(false); // Not deleted
        
        when(policyService.hasAccess(testPolicies, path + "/" + key, "update")).thenReturn(true);
        when(secretRepository.findByPathAndKeyAndVersion(path, key, version)).thenReturn(Optional.of(secret));
        
        // Act
        boolean result = secretService.restoreSecretVersion(path, key, version, testPolicies);
        
        // Assert
        assertFalse(result);
        
        verify(secretRepository, never()).save(any());
    }
}

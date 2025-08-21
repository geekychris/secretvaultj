package com.example.vault.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig
class EncryptionServiceTest {
    
    private EncryptionService encryptionService;
    
    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
        // Set a test encryption key
        ReflectionTestUtils.setField(encryptionService, "encryptionKey", "testEncryptionKey32CharsLong123!");
    }
    
    @Test
    void encrypt_ValidPlaintext_ReturnsEncryptedString() {
        // Arrange
        String plaintext = "This is a secret message";
        
        // Act
        String encrypted = encryptionService.encrypt(plaintext);
        
        // Assert
        assertNotNull(encrypted);
        assertNotEquals(plaintext, encrypted);
        assertTrue(encrypted.length() > 0);
    }
    
    @Test
    void decrypt_ValidEncryptedText_ReturnsOriginalPlaintext() {
        // Arrange
        String plaintext = "This is a secret message";
        String encrypted = encryptionService.encrypt(plaintext);
        
        // Act
        String decrypted = encryptionService.decrypt(encrypted);
        
        // Assert
        assertEquals(plaintext, decrypted);
    }
    
    @Test
    void encryptDecrypt_MultipleRounds_ConsistentResults() {
        // Arrange
        String plaintext = "Sensitive database password: mySecretPass123!";
        
        // Act & Assert - Multiple rounds of encryption/decryption
        for (int i = 0; i < 5; i++) {
            String encrypted = encryptionService.encrypt(plaintext);
            String decrypted = encryptionService.decrypt(encrypted);
            assertEquals(plaintext, decrypted);
            
            // Each encryption should produce different ciphertext due to random IV
            String encrypted2 = encryptionService.encrypt(plaintext);
            assertNotEquals(encrypted, encrypted2);
            
            // But both should decrypt to the same plaintext
            String decrypted2 = encryptionService.decrypt(encrypted2);
            assertEquals(plaintext, decrypted2);
        }
    }
    
    @Test
    void encrypt_EmptyString_SuccessfullyEncrypts() {
        // Arrange
        String plaintext = "";
        
        // Act
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);
        
        // Assert
        assertEquals(plaintext, decrypted);
    }
    
    @Test
    void encrypt_UnicodeCharacters_SuccessfullyEncrypts() {
        // Arrange
        String plaintext = "🔒 Secret with émojis and spéciál chars: ñáéíóú 中文 العربية";
        
        // Act
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);
        
        // Assert
        assertEquals(plaintext, decrypted);
    }
    
    @Test
    void encrypt_VeryLongString_SuccessfullyEncrypts() {
        // Arrange
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("This is a long string for testing encryption performance. ");
        }
        String plaintext = sb.toString();
        
        // Act
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);
        
        // Assert
        assertEquals(plaintext, decrypted);
    }
    
    @Test
    void decrypt_InvalidBase64_ThrowsException() {
        // Arrange
        String invalidBase64 = "This is not valid base64!@#$";
        
        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            encryptionService.decrypt(invalidBase64);
        });
    }
    
    @Test
    void decrypt_InvalidEncryptedData_ThrowsException() {
        // Arrange
        String invalidEncrypted = "VGhpcyBpcyBub3QgdmFsaWQgZW5jcnlwdGVkIGRhdGE="; // Valid base64 but not encrypted data
        
        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            encryptionService.decrypt(invalidEncrypted);
        });
    }
    
    @Test
    void generateKey_CreatesValidKey() {
        // Act
        String generatedKey = encryptionService.generateKey();
        
        // Assert
        assertNotNull(generatedKey);
        assertTrue(generatedKey.length() > 0);
        
        // Test that the generated key can be used for encryption
        // Note: In a real scenario, you'd need to set this key in the service
        // For this test, we'll just verify it's a valid base64 string
        assertDoesNotThrow(() -> {
            java.util.Base64.getDecoder().decode(generatedKey);
        });
    }
    
    @Test
    void encrypt_NullInput_ThrowsException() {
        // Act & Assert
        assertThrows(Exception.class, () -> {
            encryptionService.encrypt(null);
        });
    }
}

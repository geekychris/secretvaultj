package com.example.vault.service;

import com.example.vault.entity.Identity;
import com.example.vault.entity.Policy;
import com.example.vault.repository.IdentityRepository;
import com.example.vault.security.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    
    @Mock
    private IdentityRepository identityRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtTokenUtil jwtTokenUtil;
    
    @InjectMocks
    private AuthenticationService authenticationService;
    
    private Identity testIdentity;
    private Policy testPolicy;
    
    @BeforeEach
    void setUp() {
        testIdentity = new Identity("testuser", "$2a$10$hashedPassword", Identity.IdentityType.USER);
        testIdentity.setId(1L);
        testIdentity.setEnabled(true);
        
        testPolicy = new Policy("test-policy", "Test policy");
        testPolicy.setRules(Set.of("read:secret/*"));
        
        testIdentity.setPolicies(Set.of(testPolicy));
    }
    
    @Test
    void authenticate_Success() {
        // Arrange
        String username = "testuser";
        String password = "plainPassword";
        String expectedToken = "jwt-token-123";
        
        when(identityRepository.findByNameWithPolicies(username)).thenReturn(Optional.of(testIdentity));
        when(passwordEncoder.matches(password, testIdentity.getPasswordHash())).thenReturn(true);
        when(jwtTokenUtil.generateToken(eq(username), eq("USER"), any(List.class))).thenReturn(expectedToken);
        when(identityRepository.save(any(Identity.class))).thenReturn(testIdentity);
        
        // Act
        Optional<String> result = authenticationService.authenticate(username, password);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(expectedToken, result.get());
        
        verify(identityRepository).findByNameWithPolicies(username);
        verify(passwordEncoder).matches(password, testIdentity.getPasswordHash());
        verify(jwtTokenUtil).generateToken(eq(username), eq("USER"), any(List.class));
        verify(identityRepository).save(testIdentity);
        
        // Verify that last login time was updated
        assertNotNull(testIdentity.getLastLoginAt());
    }
    
    @Test
    void authenticate_UserNotFound_ReturnsEmpty() {
        // Arrange
        String username = "nonexistent";
        String password = "password";
        
        when(identityRepository.findByNameWithPolicies(username)).thenReturn(Optional.empty());
        
        // Act
        Optional<String> result = authenticationService.authenticate(username, password);
        
        // Assert
        assertTrue(result.isEmpty());
        
        verify(identityRepository).findByNameWithPolicies(username);
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(jwtTokenUtil);
        verify(identityRepository, never()).save(any());
    }
    
    @Test
    void authenticate_UserDisabled_ReturnsEmpty() {
        // Arrange
        String username = "testuser";
        String password = "password";
        
        testIdentity.setEnabled(false);
        
        when(identityRepository.findByNameWithPolicies(username)).thenReturn(Optional.of(testIdentity));
        
        // Act
        Optional<String> result = authenticationService.authenticate(username, password);
        
        // Assert
        assertTrue(result.isEmpty());
        
        verify(identityRepository).findByNameWithPolicies(username);
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(jwtTokenUtil);
        verify(identityRepository, never()).save(any());
    }
    
    @Test
    void authenticate_InvalidPassword_ReturnsEmpty() {
        // Arrange
        String username = "testuser";
        String password = "wrongPassword";
        
        when(identityRepository.findByNameWithPolicies(username)).thenReturn(Optional.of(testIdentity));
        when(passwordEncoder.matches(password, testIdentity.getPasswordHash())).thenReturn(false);
        
        // Act
        Optional<String> result = authenticationService.authenticate(username, password);
        
        // Assert
        assertTrue(result.isEmpty());
        
        verify(identityRepository).findByNameWithPolicies(username);
        verify(passwordEncoder).matches(password, testIdentity.getPasswordHash());
        verifyNoInteractions(jwtTokenUtil);
        verify(identityRepository, never()).save(any());
    }
    
    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        // Arrange
        String token = "valid-token";
        String username = "testuser";
        
        when(jwtTokenUtil.getUsernameFromToken(token)).thenReturn(username);
        when(jwtTokenUtil.validateToken(token, username)).thenReturn(true);
        
        // Act
        boolean result = authenticationService.validateToken(token);
        
        // Assert
        assertTrue(result);
        
        verify(jwtTokenUtil).getUsernameFromToken(token);
        verify(jwtTokenUtil).validateToken(token, username);
    }
    
    @Test
    void validateToken_InvalidToken_ReturnsFalse() {
        // Arrange
        String token = "invalid-token";
        
        when(jwtTokenUtil.getUsernameFromToken(token)).thenThrow(new RuntimeException("Invalid token"));
        
        // Act
        boolean result = authenticationService.validateToken(token);
        
        // Assert
        assertFalse(result);
        
        verify(jwtTokenUtil).getUsernameFromToken(token);
        verify(jwtTokenUtil, never()).validateToken(any(), any());
    }
    
    @Test
    void getIdentityFromToken_ValidToken_ReturnsIdentity() {
        // Arrange
        String token = "valid-token";
        String username = "testuser";
        
        when(jwtTokenUtil.getUsernameFromToken(token)).thenReturn(username);
        when(jwtTokenUtil.validateToken(token, username)).thenReturn(true);
        when(identityRepository.findByNameWithPolicies(username)).thenReturn(Optional.of(testIdentity));
        
        // Act
        Optional<Identity> result = authenticationService.getIdentityFromToken(token);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(testIdentity, result.get());
        
        verify(jwtTokenUtil).getUsernameFromToken(token);
        verify(jwtTokenUtil).validateToken(token, username);
        verify(identityRepository).findByNameWithPolicies(username);
    }
    
    @Test
    void getIdentityFromToken_InvalidToken_ReturnsEmpty() {
        // Arrange
        String token = "invalid-token";
        String username = "testuser";
        
        when(jwtTokenUtil.getUsernameFromToken(token)).thenReturn(username);
        when(jwtTokenUtil.validateToken(token, username)).thenReturn(false);
        
        // Act
        Optional<Identity> result = authenticationService.getIdentityFromToken(token);
        
        // Assert
        assertTrue(result.isEmpty());
        
        verify(jwtTokenUtil).getUsernameFromToken(token);
        verify(jwtTokenUtil).validateToken(token, username);
        verifyNoInteractions(identityRepository);
    }
    
    @Test
    void getPoliciesFromToken_ValidToken_ReturnsPolicies() {
        // Arrange
        String token = "valid-token";
        List<String> expectedPolicies = List.of("policy1", "policy2");
        
        when(jwtTokenUtil.getPoliciesFromToken(token)).thenReturn(expectedPolicies);
        
        // Act
        List<String> result = authenticationService.getPoliciesFromToken(token);
        
        // Assert
        assertEquals(expectedPolicies, result);
        
        verify(jwtTokenUtil).getPoliciesFromToken(token);
    }
    
    @Test
    void getPoliciesFromToken_InvalidToken_ReturnsEmptyList() {
        // Arrange
        String token = "invalid-token";
        
        when(jwtTokenUtil.getPoliciesFromToken(token)).thenThrow(new RuntimeException("Invalid token"));
        
        // Act
        List<String> result = authenticationService.getPoliciesFromToken(token);
        
        // Assert
        assertTrue(result.isEmpty());
        
        verify(jwtTokenUtil).getPoliciesFromToken(token);
    }
}

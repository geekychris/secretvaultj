package com.example.vault.service;

import com.example.vault.dto.CreateUserRequest;
import com.example.vault.dto.UserResponse;
import com.example.vault.entity.Identity;
import com.example.vault.entity.Policy;
import com.example.vault.repository.IdentityRepository;
import com.example.vault.repository.PolicyRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityServiceTest {

    @Mock
    private IdentityRepository identityRepository;

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private IdentityService identityService;

    private Policy developerPolicy;
    private Policy adminPolicy;

    @BeforeEach
    void setUp() {
        developerPolicy = new Policy("developer", "Developer policy");
        developerPolicy.setId(1L);
        
        adminPolicy = new Policy("admin", "Admin policy");
        adminPolicy.setId(2L);
    }

    @Test
    void createUser_Success() {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setType("USER");
        request.setPolicies(List.of("developer"));
        request.setEnabled(true);

        when(identityRepository.existsByName("testuser")).thenReturn(false);
        when(policyRepository.findByName("developer")).thenReturn(Optional.of(developerPolicy));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        Identity savedIdentity = new Identity("testuser", "encodedPassword", Identity.IdentityType.USER);
        savedIdentity.setId(1L);
        savedIdentity.setPolicies(Set.of(developerPolicy));
        savedIdentity.setCreatedAt(LocalDateTime.now());
        savedIdentity.setUpdatedAt(LocalDateTime.now());

        when(identityRepository.save(any(Identity.class))).thenReturn(savedIdentity);

        // When
        UserResponse result = identityService.createUser(request);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("USER", result.getType());
        assertTrue(result.getEnabled());
        assertEquals(List.of("developer"), result.getPolicies());

        verify(identityRepository).existsByName("testuser");
        verify(identityRepository).save(any(Identity.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void createUser_UsernameAlreadyExists() {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("existinguser");
        request.setPassword("password123");
        request.setType("USER");
        request.setPolicies(List.of("developer"));

        when(identityRepository.existsByName("existinguser")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> identityService.createUser(request)
        );

        assertEquals("Username 'existinguser' already exists", exception.getMessage());
        verify(identityRepository).existsByName("existinguser");
        verify(identityRepository, never()).save(any(Identity.class));
    }

    @Test
    void createUser_InvalidIdentityType() {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setType("INVALID_TYPE");
        request.setPolicies(List.of("developer"));

        when(identityRepository.existsByName("testuser")).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> identityService.createUser(request)
        );

        assertTrue(exception.getMessage().contains("Invalid identity type"));
        verify(identityRepository, never()).save(any(Identity.class));
    }

    @Test
    void createUser_PolicyNotFound() {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setType("USER");
        request.setPolicies(List.of("nonexistent"));

        when(identityRepository.existsByName("testuser")).thenReturn(false);
        when(policyRepository.findByName("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> identityService.createUser(request)
        );

        assertEquals("Policy not found: nonexistent", exception.getMessage());
        verify(identityRepository, never()).save(any(Identity.class));
    }

    @Test
    void getUserByUsername_Success() {
        // Given
        String username = "testuser";
        Identity identity = new Identity(username, "encodedPassword", Identity.IdentityType.USER);
        identity.setId(1L);
        identity.setPolicies(Set.of(developerPolicy));
        identity.setCreatedAt(LocalDateTime.now());
        identity.setUpdatedAt(LocalDateTime.now());

        when(identityRepository.findByNameWithPolicies(username)).thenReturn(Optional.of(identity));

        // When
        Optional<UserResponse> result = identityService.getUserByUsername(username);

        // Then
        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsername());
        assertEquals("USER", result.get().getType());
        assertEquals(List.of("developer"), result.get().getPolicies());
    }

    @Test
    void getUserByUsername_NotFound() {
        // Given
        String username = "nonexistent";
        when(identityRepository.findByNameWithPolicies(username)).thenReturn(Optional.empty());

        // When
        Optional<UserResponse> result = identityService.getUserByUsername(username);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void deleteUser_Success() {
        // Given
        String username = "testuser";
        Identity identity = new Identity(username, "encodedPassword", Identity.IdentityType.USER);
        identity.setId(1L);

        when(identityRepository.findByName(username)).thenReturn(Optional.of(identity));

        // When
        boolean result = identityService.deleteUser(username);

        // Then
        assertTrue(result);
        verify(identityRepository).delete(identity);
    }

    @Test
    void deleteUser_AdminUserProtected() {
        // Given
        String username = "admin";
        Identity identity = new Identity(username, "encodedPassword", Identity.IdentityType.ADMIN);
        identity.setId(1L);

        when(identityRepository.findByName(username)).thenReturn(Optional.of(identity));

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> identityService.deleteUser(username)
        );

        assertEquals("Cannot delete the admin user", exception.getMessage());
        verify(identityRepository, never()).delete(any(Identity.class));
    }

    @Test
    void toggleUserStatus_Success() {
        // Given
        String username = "testuser";
        Identity identity = new Identity(username, "encodedPassword", Identity.IdentityType.USER);
        identity.setId(1L);
        identity.setEnabled(true);

        when(identityRepository.findByName(username)).thenReturn(Optional.of(identity));
        when(identityRepository.save(any(Identity.class))).thenReturn(identity);

        // When
        boolean result = identityService.toggleUserStatus(username, false);

        // Then
        assertTrue(result);
        assertFalse(identity.getEnabled());
        verify(identityRepository).save(identity);
    }

    @Test
    void toggleUserStatus_AdminUserProtected() {
        // Given
        String username = "admin";
        Identity identity = new Identity(username, "encodedPassword", Identity.IdentityType.ADMIN);
        identity.setId(1L);
        identity.setEnabled(true);

        when(identityRepository.findByName(username)).thenReturn(Optional.of(identity));

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> identityService.toggleUserStatus(username, false)
        );

        assertEquals("Cannot disable the admin user", exception.getMessage());
        verify(identityRepository, never()).save(any(Identity.class));
    }

    @Test
    void updateUser_Success() {
        // Given
        String username = "testuser";
        Identity existingIdentity = new Identity(username, "oldPassword", Identity.IdentityType.USER);
        existingIdentity.setId(1L);
        existingIdentity.setPolicies(Set.of(developerPolicy));

        CreateUserRequest updateRequest = new CreateUserRequest();
        updateRequest.setPassword("newPassword123");
        updateRequest.setType("USER");
        updateRequest.setPolicies(List.of("developer", "admin"));
        updateRequest.setEnabled(false);

        when(identityRepository.findByName(username)).thenReturn(Optional.of(existingIdentity));
        when(policyRepository.findByName("developer")).thenReturn(Optional.of(developerPolicy));
        when(policyRepository.findByName("admin")).thenReturn(Optional.of(adminPolicy));
        when(passwordEncoder.encode("newPassword123")).thenReturn("newEncodedPassword");
        when(identityRepository.save(any(Identity.class))).thenReturn(existingIdentity);

        // When
        Optional<UserResponse> result = identityService.updateUser(username, updateRequest);

        // Then
        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsername());
        assertFalse(result.get().getEnabled());

        verify(passwordEncoder).encode("newPassword123");
        verify(identityRepository).save(existingIdentity);
    }
}

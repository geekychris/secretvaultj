package com.example.vault.controller;

import com.example.vault.dto.CreateUserRequest;
import com.example.vault.dto.UserResponse;
import com.example.vault.service.IdentityService;
import com.example.vault.service.PolicyService;
import com.example.vault.service.AuthenticationService;
import com.example.vault.security.JwtTokenUtil;
import com.example.vault.security.JwtAuthenticationFilter;
import com.example.vault.config.VaadinSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IdentityService identityService;

    @MockBean
    private PolicyService policyService;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(authorities = "admin")
    void createUser_Success() throws Exception {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setType("USER");
        request.setPolicies(List.of("developer"));
        request.setEnabled(true);
        request.setDescription("Test user");

        UserResponse userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setUsername("testuser");
        userResponse.setType("USER");
        userResponse.setEnabled(true);
        userResponse.setPolicies(List.of("developer"));
        userResponse.setCreatedAt(LocalDateTime.now());

        when(identityService.createUser(any(CreateUserRequest.class))).thenReturn(userResponse);

        // When & Then
        mockMvc.perform(post("/v1/admin/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User created successfully"))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.type").value("USER"))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    @WithMockUser(authorities = "admin")
    void createUser_ValidationError() throws Exception {
        // Given - Invalid request (missing required fields)
        CreateUserRequest request = new CreateUserRequest();
        // Missing username, password, type, and policies

        // When & Then
        mockMvc.perform(post("/v1/admin/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "admin")
    void createUser_UsernameAlreadyExists() throws Exception {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("existinguser");
        request.setPassword("password123");
        request.setType("USER");
        request.setPolicies(List.of("developer"));
        request.setEnabled(true);

        when(identityService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new IllegalArgumentException("Username 'existinguser' already exists"));

        // When & Then
        mockMvc.perform(post("/v1/admin/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Username 'existinguser' already exists"));
    }

    @Test
    @WithMockUser(authorities = "developer") // Non-admin user
    void createUser_AccessDenied() throws Exception {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setType("USER");
        request.setPolicies(List.of("developer"));

        // When & Then - Now that we have @EnableMethodSecurity, this should be forbidden
        mockMvc.perform(post("/v1/admin/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "admin")
    void getUser_Success() throws Exception {
        // Given
        String username = "testuser";
        UserResponse userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setUsername(username);
        userResponse.setType("USER");
        userResponse.setEnabled(true);
        userResponse.setPolicies(List.of("developer"));

        when(identityService.getUserByUsername(username)).thenReturn(Optional.of(userResponse));

        // When & Then
        mockMvc.perform(get("/v1/admin/users/{username}", username))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.type").value("USER"));
    }

    @Test
    @WithMockUser(authorities = "admin")
    void getUser_NotFound() throws Exception {
        // Given
        String username = "nonexistent";
        when(identityService.getUserByUsername(username)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/v1/admin/users/{username}", username))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    @WithMockUser(authorities = "admin")
    void getAllUsers_Success() throws Exception {
        // Given
        UserResponse user1 = new UserResponse();
        user1.setId(1L);
        user1.setUsername("admin");
        user1.setType("ADMIN");
        user1.setPolicies(List.of("admin"));

        UserResponse user2 = new UserResponse();
        user2.setId(2L);
        user2.setUsername("developer1");
        user2.setType("USER");
        user2.setPolicies(List.of("developer"));

        when(identityService.getAllUsers()).thenReturn(List.of(user1, user2));

        // When & Then
        mockMvc.perform(get("/v1/admin/users"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].username").value("admin"))
                .andExpect(jsonPath("$.data[1].username").value("developer1"));
    }

    @Test
    @WithMockUser(authorities = "admin")
    void updateUser_Success() throws Exception {
        // Given
        String username = "testuser";
        CreateUserRequest updateRequest = new CreateUserRequest();
        updateRequest.setUsername(username); // Required field
        updateRequest.setPassword("newpassword123");
        updateRequest.setType("USER"); // Required field
        updateRequest.setPolicies(List.of("developer", "readonly"));
        updateRequest.setEnabled(true);

        UserResponse updatedUser = new UserResponse();
        updatedUser.setId(1L);
        updatedUser.setUsername(username);
        updatedUser.setType("USER");
        updatedUser.setEnabled(true);
        updatedUser.setPolicies(List.of("developer", "readonly"));

        when(identityService.updateUser(eq(username), any(CreateUserRequest.class)))
                .thenReturn(Optional.of(updatedUser));

        // When & Then
        mockMvc.perform(put("/v1/admin/users/{username}", username)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.policies.length()").value(2));
    }

    @Test
    @WithMockUser(authorities = "admin")
    void deleteUser_Success() throws Exception {
        // Given
        String username = "testuser";
        when(identityService.deleteUser(username)).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/v1/admin/users/{username}", username)
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User deleted successfully"));
    }

    @Test
    @WithMockUser(authorities = "admin")
    void toggleUserStatus_Success() throws Exception {
        // Given
        String username = "testuser";
        when(identityService.toggleUserStatus(username, false)).thenReturn(true);

        // When & Then
        mockMvc.perform(patch("/v1/admin/users/{username}/status", username)
                .with(csrf())
                .param("enabled", "false"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User disabled successfully"));
    }
}

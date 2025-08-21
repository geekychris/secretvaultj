package com.example.vault;

import com.example.vault.dto.AuthRequest;
import com.example.vault.dto.SecretRequest;
import com.example.vault.entity.Identity;
import com.example.vault.entity.Policy;
import com.example.vault.repository.IdentityRepository;
import com.example.vault.repository.PolicyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class VaultApplicationTests {
    
    @Autowired
    private WebApplicationContext webApplicationContext;
    
    @Autowired
    private IdentityRepository identityRepository;
    
    @Autowired
    private PolicyRepository policyRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();
    private String authToken;
    
    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Create test policy
        Policy testPolicy = new Policy("test-policy", "Test policy for integration tests");
        testPolicy.getRules().addAll(Set.of(
            "create:*",
            "read:*",
            "update:*",
            "delete:*",
            "list:*"
        ));
        policyRepository.save(testPolicy);
        
        // Create test user
        Identity testUser = new Identity("testuser", passwordEncoder.encode("password123"), Identity.IdentityType.USER);
        testUser.getPolicies().add(testPolicy);
        identityRepository.save(testUser);
        
        // Authenticate and get token
        AuthRequest authRequest = new AuthRequest("testuser", "password123");
        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        String responseBody = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
        authToken = (String) response.get("token");
    }
    
    @Test
    void contextLoads() {
        // Test that the Spring context loads successfully
    }
    
    @Test
    void secretController_Test_ReturnsOk() throws Exception {
        mockMvc.perform(get("/v1/secret/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("Controller is working!"));
    }
    
    @Test
    void healthEndpoint_ReturnsHealthy() throws Exception {
        mockMvc.perform(get("/v1/sys/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("healthy"));
    }
    
    @Test
    void authLogin_ValidCredentials_ReturnsToken() throws Exception {
        AuthRequest authRequest = new AuthRequest("testuser", "password123");
        
        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token_type").value("Bearer"));
    }
    
    @Test
    void authLogin_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        AuthRequest authRequest = new AuthRequest("testuser", "wrongpassword");
        
        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
    
    @Test
    void secretOperations_FullCRUDCycle_Success() throws Exception {
        String path = "myapp";
        String key = "database-password";
        
        // Create secret
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("environment", "production");
        SecretRequest createRequest = new SecretRequest("mysecretpassword", metadata);
        
        mockMvc.perform(post("/v1/secret/" + path)
                .param("key", key)
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.path").value(path + "/" + key));
        
        // Read secret
        mockMvc.perform(get("/v1/secret/" + path)
                .param("key", key)
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.value").value("mysecretpassword"))
                .andExpect(jsonPath("$.data.version").value(1));
        
        // Update secret
        SecretRequest updateRequest = new SecretRequest("updatedsecretpassword", metadata);
        
        mockMvc.perform(put("/v1/secret/" + path)
                .param("key", key)
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.version").value(2));
        
        // Read updated secret
        mockMvc.perform(get("/v1/secret/" + path)
                .param("key", key)
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value").value("updatedsecretpassword"))
                .andExpect(jsonPath("$.data.version").value(2));
        
        // Delete secret
        mockMvc.perform(delete("/v1/secret/" + path)
                .param("key", key)
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        
        // Verify secret is deleted
        mockMvc.perform(get("/v1/secret/" + path)
                .param("key", key)
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void secretOperations_WithoutAuth_ReturnsUnauthorized() throws Exception {
        String path = "myapp";
        String key = "database-password";
        SecretRequest request = new SecretRequest("mysecretpassword", null);
        
        mockMvc.perform(post("/v1/secret/" + path)
                .param("key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
    
    @Test
    void secretOperations_InvalidToken_ReturnsUnauthorized() throws Exception {
        String path = "myapp";
        String key = "database-password";
        SecretRequest request = new SecretRequest("mysecretpassword", null);
        
        mockMvc.perform(post("/v1/secret/" + path)
                .param("key", key)
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
    
    @Test
    void systemStatus_ReturnsValidStatus() throws Exception {
        mockMvc.perform(get("/v1/sys/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialized").value(true))
                .andExpect(jsonPath("$.sealed").value(false))
                .andExpect(jsonPath("$.replication_enabled").exists())
                .andExpect(jsonPath("$.instance_id").exists());
    }
    
    // ============= VERSIONING INTEGRATION TESTS =============
    
    @Test
    void secretVersioning_CompleteWorkflow_Success() throws Exception {
        String path = "demo";
        String key = "versioned-secret";
        
        // Step 1: Create initial secret (version 1)
        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("version_note", "First version");
        metadata1.put("env", "demo");
        SecretRequest createRequest = new SecretRequest("initial-value-v1", metadata1);
        
        mockMvc.perform(post("/v1/secret/" + path)
                .param("key", key)
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.path").value(path + "/" + key));
        
        // Step 2: Read initial secret (should be version 1)
        mockMvc.perform(get("/v1/secret/" + path)
                .param("key", key)
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.value").value("initial-value-v1"))
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.metadata.version_note").value("First version"));
        
        // Step 3: Update secret (create version 2)
        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("version_note", "Second version");
        metadata2.put("env", "demo");
        SecretRequest updateRequest1 = new SecretRequest("updated-value-v2", metadata2);
        
        mockMvc.perform(put("/v1/secret/" + path)
                .param("key", key)
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.version").value(2));
        
        // Step 4: Update secret again (create version 3)
        Map<String, Object> metadata3 = new HashMap<>();
        metadata3.put("version_note", "Third version");
        metadata3.put("env", "demo");
        metadata3.put("final", "true");
        SecretRequest updateRequest2 = new SecretRequest("final-value-v3", metadata3);
        
        mockMvc.perform(put("/v1/secret/" + path)
                .param("key", key)
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.version").value(3));
        
        // Step 5: Read latest version (should be version 3)
        mockMvc.perform(get("/v1/secret/" + path)
                .param("key", key)
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value").value("final-value-v3"))
                .andExpect(jsonPath("$.data.version").value(3))
                .andExpect(jsonPath("$.data.metadata.final").value("true"));
        
        // Step 6: Read specific version (version 1)
        mockMvc.perform(get("/v1/secret/" + path)
                .param("key", key)
                .param("version", "1")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value").value("initial-value-v1"))
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.metadata.version_note").value("First version"));
        
        // Step 7: List all versions
        mockMvc.perform(get("/v1/secret/versions/" + path)
                .param("key", key)
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.total_versions").value(3))
                .andExpect(jsonPath("$.versions").isArray())
                .andExpect(jsonPath("$.versions[0].version").value(3)) // Latest first
                .andExpect(jsonPath("$.versions[1].version").value(2))
                .andExpect(jsonPath("$.versions[2].version").value(1));
        
        // Step 8: Get version information
        mockMvc.perform(get("/v1/secret/version-info/" + path)
                .param("key", key)
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total_versions").value(3))
                .andExpect(jsonPath("$.data.earliest_version").value(1))
                .andExpect(jsonPath("$.data.latest_version").value(3));
        
        // Step 9: Get version range (versions 1-2)
        mockMvc.perform(get("/v1/secret/version-range/" + path)
                .param("key", key)
                .param("startVersion", "1")
                .param("endVersion", "2")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.versions[0].version").value(2)) // Desc order
                .andExpect(jsonPath("$.versions[0].value").value("updated-value-v2"))
                .andExpect(jsonPath("$.versions[1].version").value(1))
                .andExpect(jsonPath("$.versions[1].value").value("initial-value-v1"));
        
        // Step 10: Delete specific version (version 2)
        mockMvc.perform(delete("/v1/secret/version/" + path)
                .param("key", key)
                .param("version", "2")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.version").value(2));
        
        // Step 11: Verify version 2 is no longer in active list
        mockMvc.perform(get("/v1/secret/versions/" + path)
                .param("key", key)
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_versions").value(2)) // Should be 2 now
                .andExpect(jsonPath("$.versions[0].version").value(3))
                .andExpect(jsonPath("$.versions[1].version").value(1)); // Version 2 should be gone
        
        // Step 12: Restore version 2
        mockMvc.perform(post("/v1/secret/restore-version/" + path)
                .param("key", key)
                .param("version", "2")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.version").value(2));
        
        // Step 13: Verify version 2 is restored
        mockMvc.perform(get("/v1/secret/versions/" + path)
                .param("key", key)
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_versions").value(3)) // Should be 3 again
                .andExpect(jsonPath("$.versions[1].version").value(2)); // Version 2 should be back
    }
    
    @Test
    void secretVersioning_SpecificVersionRead_Success() throws Exception {
        String path = "version-test";
        String key = "multi-version-secret";
        
        // Create initial version
        SecretRequest request1 = new SecretRequest("value1", Map.of("note", "version1"));
        mockMvc.perform(post("/v1/secret/" + path)
                .param("key", key)
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());
        
        // Create second version
        SecretRequest request2 = new SecretRequest("value2", Map.of("note", "version2"));
        mockMvc.perform(put("/v1/secret/" + path)
                .param("key", key)
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk());
        
        // Read without version (should get latest - version 2)
        mockMvc.perform(get("/v1/secret/" + path)
                .param("key", key)
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value").value("value2"))
                .andExpect(jsonPath("$.data.version").value(2));
        
        // Read specific version 1
        mockMvc.perform(get("/v1/secret/" + path)
                .param("key", key)
                .param("version", "1")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value").value("value1"))
                .andExpect(jsonPath("$.data.version").value(1));
    }
    
    @Test
    void secretVersioning_InvalidVersionRange_ReturnsBadRequest() throws Exception {
        String path = "range-test";
        String key = "range-secret";
        
        // Create a secret first
        SecretRequest request = new SecretRequest("test-value", null);
        mockMvc.perform(post("/v1/secret/" + path)
                .param("key", key)
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        
        // Try invalid range (start > end) - This should trigger service layer validation
        mockMvc.perform(get("/v1/secret/version-range/" + path)
                .param("key", key)
                .param("startVersion", "3")
                .param("endVersion", "1")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void secretVersioning_DeleteNonExistentVersion_ReturnsNotFound() throws Exception {
        String path = "delete-test";
        String key = "delete-secret";
        
        // Create a secret first (version 1)
        SecretRequest request = new SecretRequest("test-value", null);
        mockMvc.perform(post("/v1/secret/" + path)
                .param("key", key)
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        
        // Try to delete non-existent version
        mockMvc.perform(delete("/v1/secret/version/" + path)
                .param("key", key)
                .param("version", "999")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void secretVersioning_WithoutAuthentication_ReturnsForbidden() throws Exception {
        // Test version endpoints without authentication
        mockMvc.perform(get("/v1/secret/versions/test")
                .param("key", "test-key"))
                .andExpect(status().isForbidden());
        
        mockMvc.perform(get("/v1/secret/version-info/test")
                .param("key", "test-key"))
                .andExpect(status().isForbidden());
        
        mockMvc.perform(get("/v1/secret/version-range/test")
                .param("key", "test-key"))
                .andExpect(status().isForbidden());
        
        mockMvc.perform(delete("/v1/secret/version/test")
                .param("key", "test-key")
                .param("version", "1"))
                .andExpect(status().isForbidden());
        
        mockMvc.perform(post("/v1/secret/restore-version/test")
                .param("key", "test-key")
                .param("version", "1"))
                .andExpect(status().isForbidden());
    }
}

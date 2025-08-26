package com.example.vault.controller;

import com.example.vault.dto.SecretRequest;
import com.example.vault.entity.Identity;
import com.example.vault.service.AuthenticationService;
import com.example.vault.service.SecretService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/secret")
@Tag(name = "Secrets", description = "Secret storage, retrieval, and management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class SecretsController {
    
    private static final Logger logger = LoggerFactory.getLogger(SecretsController.class);
    
    @Autowired
    private SecretService secretService;
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @Operation(
            summary = "Create a new secret",
            description = "Creates a new secret at the specified path with the given key and value"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Secret created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "success": true,
                                      "message": "Secret created successfully",
                                      "path": "app/config/database/password",
                                      "timestamp": "2024-01-15T10:30:00"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PostMapping("/{path:.+}")
    public ResponseEntity<Map<String, Object>> createSecret(
            @Parameter(description = "Secret storage path", example = "app/config/database")
            @PathVariable String path,
            @Parameter(description = "Secret key name", example = "password")
            @RequestParam String key,
            @Valid @RequestBody SecretRequest request,
            @Parameter(description = "Bearer token for authentication")
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        String token = extractToken(authHeader);
        Optional<Identity> identityOpt = authenticationService.getIdentityFromToken(token);
        
        if (identityOpt.isEmpty()) {
            throw new SecurityException("Invalid or expired token");
        }
        
        List<String> policies = authenticationService.getPoliciesFromToken(token);
        
        secretService.createSecret(path, key, request.getValue(), request.getMetadata(), 
                                  identityOpt.get(), policies);
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("success", true);
        response.put("message", "Secret created successfully");
        response.put("path", path + "/" + key);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(
            summary = "Retrieve a secret",
            description = "Retrieves a secret by path and key, optionally specifying a version"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Secret retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "success": true,
                                      "path": "app/config/database/password",
                                      "data": {
                                        "value": "mysecretpassword",
                                        "version": 3,
                                        "metadata": {"environment": "production"}
                                      },
                                      "timestamp": "2024-01-15T10:30:00"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Secret not found"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @GetMapping("/{path:.+}")
    public ResponseEntity<Map<String, Object>> getSecret(
            @Parameter(description = "Secret storage path", example = "app/config/database")
            @PathVariable String path,
            @Parameter(description = "Secret key name", example = "password")
            @RequestParam String key,
            @Parameter(description = "Optional version number to retrieve specific version")
            @RequestParam(required = false) Integer version,
            @Parameter(description = "Bearer token for authentication")
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        String token = extractToken(authHeader);
        List<String> policies = authenticationService.getPoliciesFromToken(token);
        
        if (!authenticationService.validateToken(token)) {
            throw new SecurityException("Invalid or expired token");
        }
        
        Optional<Map<String, Object>> secretOpt = secretService.getSecret(path, key, version, policies);
        
        if (secretOpt.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("timestamp", LocalDateTime.now());
            response.put("success", false);
            response.put("message", "Secret not found");
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("success", true);
        response.put("path", path + "/" + key);
        response.put("data", secretOpt.get());
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(
            summary = "Update an existing secret",
            description = "Updates an existing secret, creating a new version"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Secret updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "success": true,
                                      "message": "Secret updated successfully",
                                      "path": "app/config/database/password",
                                      "version": 4,
                                      "timestamp": "2024-01-15T10:30:00"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Secret not found"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PutMapping("/{path:.+}")
    public ResponseEntity<Map<String, Object>> updateSecret(
            @Parameter(description = "Secret storage path", example = "app/config/database")
            @PathVariable String path,
            @Parameter(description = "Secret key name", example = "password")
            @RequestParam String key,
            @Valid @RequestBody SecretRequest request,
            @Parameter(description = "Bearer token for authentication")
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        String token = extractToken(authHeader);
        Optional<Identity> identityOpt = authenticationService.getIdentityFromToken(token);
        
        if (identityOpt.isEmpty()) {
            throw new SecurityException("Invalid or expired token");
        }
        
        List<String> policies = authenticationService.getPoliciesFromToken(token);
        
        Optional<com.example.vault.entity.Secret> updatedSecret = secretService.updateSecret(
            path, key, request.getValue(), request.getMetadata(), identityOpt.get(), policies);
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        
        if (updatedSecret.isPresent()) {
            response.put("success", true);
            response.put("message", "Secret updated successfully");
            response.put("path", path + "/" + key);
            response.put("version", updatedSecret.get().getVersion());
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Secret not found");
            return ResponseEntity.notFound().build();
        }
    }
    
    @Operation(
            summary = "Delete a secret",
            description = "Permanently deletes a secret and all its versions"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Secret deleted successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "success": true,
                                      "message": "Secret deleted successfully",
                                      "timestamp": "2024-01-15T10:30:00"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Secret not found"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @DeleteMapping("/{path:.+}")
    public ResponseEntity<Map<String, Object>> deleteSecret(
            @Parameter(description = "Secret storage path", example = "app/config/database")
            @PathVariable String path,
            @Parameter(description = "Secret key name", example = "password")
            @RequestParam String key,
            @Parameter(description = "Bearer token for authentication")
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        String token = extractToken(authHeader);
        List<String> policies = authenticationService.getPoliciesFromToken(token);
        
        if (!authenticationService.validateToken(token)) {
            throw new SecurityException("Invalid or expired token");
        }
        
        boolean deleted = secretService.deleteSecret(path, key, policies);
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        
        if (deleted) {
            response.put("success", true);
            response.put("message", "Secret deleted successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Secret not found");
            return ResponseEntity.notFound().build();
        }
    }
    
    @Operation(
            summary = "List secrets at a path",
            description = "Lists all secret keys available at the specified path"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Secrets listed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "success": true,
                                      "path": "app/config/database",
                                      "keys": ["password", "username", "host"],
                                      "count": 3,
                                      "timestamp": "2024-01-15T10:30:00"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @GetMapping("/list/{path:.+}")
    public ResponseEntity<Map<String, Object>> listSecrets(
            @Parameter(description = "Path to list secrets from", example = "app/config/database")
            @PathVariable String path,
            @Parameter(description = "Whether to list recursively through subdirectories")
            @RequestParam(defaultValue = "false") boolean recursive,
            @Parameter(description = "Bearer token for authentication")
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        String token = extractToken(authHeader);
        List<String> policies = authenticationService.getPoliciesFromToken(token);
        
        if (!authenticationService.validateToken(token)) {
            throw new SecurityException("Invalid or expired token");
        }
        
        List<String> secrets = secretService.listSecrets(path, recursive, policies);
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("success", true);
        response.put("path", path);
        response.put("keys", secrets);
        response.put("count", secrets.size());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/paths/{pathPrefix:.+}")
    public ResponseEntity<Map<String, Object>> listPaths(
            @PathVariable String pathPrefix,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        String token = extractToken(authHeader);
        List<String> policies = authenticationService.getPoliciesFromToken(token);
        
        if (!authenticationService.validateToken(token)) {
            throw new SecurityException("Invalid or expired token");
        }
        
        List<String> paths = secretService.listPaths(pathPrefix, policies);
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("success", true);
        response.put("prefix", pathPrefix);
        response.put("paths", paths);
        response.put("count", paths.size());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Controller is working!");
    }
    
    // ============= VERSIONING ENDPOINTS =============
    
    @GetMapping("/versions/{path:.+}")
    public ResponseEntity<Map<String, Object>> listSecretVersions(
            @PathVariable String path,
            @RequestParam String key,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        String token = extractToken(authHeader);
        List<String> policies = authenticationService.getPoliciesFromToken(token);
        
        if (!authenticationService.validateToken(token)) {
            throw new SecurityException("Invalid or expired token");
        }
        
        List<Map<String, Object>> versions = secretService.listSecretVersions(path, key, policies);
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("success", true);
        response.put("path", path + "/" + key);
        response.put("versions", versions);
        response.put("total_versions", versions.size());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/version-info/{path:.+}")
    public ResponseEntity<Map<String, Object>> getSecretVersionInfo(
            @PathVariable String path,
            @RequestParam String key,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        String token = extractToken(authHeader);
        List<String> policies = authenticationService.getPoliciesFromToken(token);
        
        if (!authenticationService.validateToken(token)) {
            throw new SecurityException("Invalid or expired token");
        }
        
        Map<String, Object> versionInfo = secretService.getSecretVersionInfo(path, key, policies);
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("success", true);
        response.put("data", versionInfo);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/version-range/{path:.+}")
    public ResponseEntity<Map<String, Object>> getSecretVersionRange(
            @PathVariable String path,
            @RequestParam String key,
            @RequestParam(required = false) Integer startVersion,
            @RequestParam(required = false) Integer endVersion,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        String token = extractToken(authHeader);
        List<String> policies = authenticationService.getPoliciesFromToken(token);
        
        if (!authenticationService.validateToken(token)) {
            throw new SecurityException("Invalid or expired token");
        }
        
        List<Map<String, Object>> versions = secretService.getSecretVersionRange(path, key, startVersion, endVersion, policies);
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("success", true);
        response.put("path", path + "/" + key);
        response.put("start_version", startVersion);
        response.put("end_version", endVersion);
        response.put("versions", versions);
        response.put("count", versions.size());
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/version/{path:.+}")
    public ResponseEntity<Map<String, Object>> deleteSecretVersion(
            @PathVariable String path,
            @RequestParam String key,
            @RequestParam Integer version,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        String token = extractToken(authHeader);
        List<String> policies = authenticationService.getPoliciesFromToken(token);
        
        if (!authenticationService.validateToken(token)) {
            throw new SecurityException("Invalid or expired token");
        }
        
        boolean deleted = secretService.deleteSecretVersion(path, key, version, policies);
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        
        if (deleted) {
            response.put("success", true);
            response.put("message", "Secret version deleted successfully");
            response.put("path", path + "/" + key);
            response.put("version", version);
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Secret version not found or already deleted");
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/restore-version/{path:.+}")
    public ResponseEntity<Map<String, Object>> restoreSecretVersion(
            @PathVariable String path,
            @RequestParam String key,
            @RequestParam Integer version,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        String token = extractToken(authHeader);
        List<String> policies = authenticationService.getPoliciesFromToken(token);
        
        if (!authenticationService.validateToken(token)) {
            throw new SecurityException("Invalid or expired token");
        }
        
        boolean restored = secretService.restoreSecretVersion(path, key, version, policies);
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        
        if (restored) {
            response.put("success", true);
            response.put("message", "Secret version restored successfully");
            response.put("path", path + "/" + key);
            response.put("version", version);
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Secret version not found or not deleted");
            return ResponseEntity.notFound().build();
        }
    }
    
    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new SecurityException("Missing or invalid Authorization header");
    }
}

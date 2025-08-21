package com.example.vault.controller;

import com.example.vault.dto.SecretRequest;
import com.example.vault.entity.Identity;
import com.example.vault.service.AuthenticationService;
import com.example.vault.service.SecretService;
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
public class SecretsController {
    
    private static final Logger logger = LoggerFactory.getLogger(SecretsController.class);
    
    @Autowired
    private SecretService secretService;
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @PostMapping("/{path:.+}")
    public ResponseEntity<Map<String, Object>> createSecret(
            @PathVariable String path,
            @RequestParam String key,
            @Valid @RequestBody SecretRequest request,
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
    
    @GetMapping("/{path:.+}")
    public ResponseEntity<Map<String, Object>> getSecret(
            @PathVariable String path,
            @RequestParam String key,
            @RequestParam(required = false) Integer version,
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
    
    @PutMapping("/{path:.+}")
    public ResponseEntity<Map<String, Object>> updateSecret(
            @PathVariable String path,
            @RequestParam String key,
            @Valid @RequestBody SecretRequest request,
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
    
    @DeleteMapping("/{path:.+}")
    public ResponseEntity<Map<String, Object>> deleteSecret(
            @PathVariable String path,
            @RequestParam String key,
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
    
    @GetMapping("/list/{path:.+}")
    public ResponseEntity<Map<String, Object>> listSecrets(
            @PathVariable String path,
            @RequestParam(defaultValue = "false") boolean recursive,
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

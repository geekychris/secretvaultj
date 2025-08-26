package com.example.vault.controller;

import com.example.vault.service.ReplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/sys")
@Tag(name = "System", description = "System health, status and information endpoints")
public class SystemController {
    
    @Autowired
    private ReplicationService replicationService;
    
    @Operation(
            summary = "System health check",
            description = "Returns the current health status of the vault system"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "System is healthy",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "status": "healthy",
                                      "version": "1.0.0",
                                      "timestamp": "2024-01-15T10:30:00"
                                    }
                                    """
                            )
                    )
            )
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("timestamp", LocalDateTime.now());
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }
    
    @Operation(
            summary = "System status information",
            description = "Returns detailed system status including initialization, seal status, and replication information"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "System status retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "initialized": true,
                                      "sealed": false,
                                      "standby": false,
                                      "replication_enabled": true,
                                      "instance_id": "vault-node-1",
                                      "timestamp": "2024-01-15T10:30:00"
                                    }
                                    """
                            )
                    )
            )
    })
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> response = new HashMap<>();
        response.put("initialized", true);
        response.put("sealed", false);
        response.put("standby", false);
        response.put("replication_enabled", replicationService.isReplicationEnabled());
        response.put("instance_id", replicationService.getInstanceId());
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
    
    @Operation(
            summary = "System version information",
            description = "Returns version information including build details and Java runtime version"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Version information retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "version": "1.0.0",
                                      "build_date": "2024-01-01",
                                      "java_version": "21.0.6"
                                    }
                                    """
                            )
                    )
            )
    })
    @GetMapping("/version")
    public ResponseEntity<Map<String, Object>> version() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "1.0.0");
        response.put("build_date", "2024-01-01");
        response.put("java_version", System.getProperty("java.version"));
        return ResponseEntity.ok(response);
    }
}

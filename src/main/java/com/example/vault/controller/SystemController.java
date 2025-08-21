package com.example.vault.controller;

import com.example.vault.service.ReplicationService;
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
public class SystemController {
    
    @Autowired
    private ReplicationService replicationService;
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("timestamp", LocalDateTime.now());
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }
    
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
    
    @GetMapping("/version")
    public ResponseEntity<Map<String, Object>> version() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "1.0.0");
        response.put("build_date", "2024-01-01");
        response.put("java_version", System.getProperty("java.version"));
        return ResponseEntity.ok(response);
    }
}

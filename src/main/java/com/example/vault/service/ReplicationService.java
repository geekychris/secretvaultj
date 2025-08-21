package com.example.vault.service;

import com.example.vault.entity.ReplicationLog;
import com.example.vault.repository.ReplicationLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReplicationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReplicationService.class);
    
    @Autowired
    private ReplicationLogRepository replicationLogRepository;
    
    @Value("${vault.replication.enabled:true}")
    private boolean replicationEnabled;
    
    @Value("${vault.replication.cleanup-days:7}")
    private int cleanupDays;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String instanceId;
    
    public ReplicationService() {
        // Generate unique instance ID
        String generatedId;
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            String timestamp = String.valueOf(System.currentTimeMillis());
            generatedId = hostName + "-" + timestamp.substring(timestamp.length() - 6);
        } catch (Exception e) {
            generatedId = "vault-" + System.currentTimeMillis();
        }
        this.instanceId = generatedId;
        logger.info("Replication service initialized with instance ID: {}", instanceId);
    }
    
    @Async
    @Transactional
    public void logOperation(ReplicationLog.EntityType entityType, Long entityId, 
                           ReplicationLog.OperationType operationType, Object entityData) {
        
        if (!replicationEnabled) {
            return;
        }
        
        try {
            String serializedData = objectMapper.writeValueAsString(entityData);
            
            ReplicationLog log = new ReplicationLog(
                entityType,
                entityId,
                operationType,
                serializedData,
                instanceId
            );
            
            replicationLogRepository.save(log);
            logger.debug("Logged replication operation: {} {} {}", operationType, entityType, entityId);
            
        } catch (Exception e) {
            logger.error("Failed to log replication operation", e);
        }
    }
    
    @Scheduled(fixedDelayString = "${vault.replication.sync-interval:30000}") // 30 seconds
    @Transactional
    public void processReplicationLogs() {
        if (!replicationEnabled) {
            return;
        }
        
        try {
            List<ReplicationLog> unprocessedLogs = replicationLogRepository
                .findUnprocessedLogsFromOtherInstances(instanceId);
            
            if (unprocessedLogs.isEmpty()) {
                return;
            }
            
            logger.info("Processing {} replication logs from other instances", unprocessedLogs.size());
            
            for (ReplicationLog log : unprocessedLogs) {
                try {
                    processReplicationLog(log);
                    log.setProcessed(true);
                    replicationLogRepository.save(log);
                } catch (Exception e) {
                    logger.error("Failed to process replication log: {}", log.getId(), e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error during replication log processing", e);
        }
    }
    
    private void processReplicationLog(ReplicationLog log) {
        logger.debug("Processing replication log: {} {} {} from instance {}", 
            log.getOperationType(), log.getEntityType(), log.getEntityId(), log.getSourceInstance());
        
        // In a real implementation, you would:
        // 1. Deserialize the entity data
        // 2. Apply the operation to the local database
        // 3. Handle conflicts and versioning
        // 4. Ensure data consistency
        
        // For this example, we'll just mark it as processed
        // The actual implementation would depend on your specific replication strategy:
        // - Last-writer-wins
        // - Vector clocks
        // - Conflict resolution policies
        // - etc.
        
        switch (log.getEntityType()) {
            case SECRET -> processSecretReplication(log);
            case IDENTITY -> processIdentityReplication(log);
            case POLICY -> processPolicyReplication(log);
        }
    }
    
    private void processSecretReplication(ReplicationLog log) {
        // Implementation would handle secret replication
        // This might involve updating local secrets based on remote changes
        logger.debug("Processing secret replication for entity {}", log.getEntityId());
    }
    
    private void processIdentityReplication(ReplicationLog log) {
        // Implementation would handle identity replication
        logger.debug("Processing identity replication for entity {}", log.getEntityId());
    }
    
    private void processPolicyReplication(ReplicationLog log) {
        // Implementation would handle policy replication
        logger.debug("Processing policy replication for entity {}", log.getEntityId());
    }
    
    @Scheduled(cron = "${vault.replication.cleanup-cron:0 0 2 * * ?}") // Daily at 2 AM
    @Transactional
    public void cleanupOldReplicationLogs() {
        if (!replicationEnabled) {
            return;
        }
        
        LocalDateTime cutoff = LocalDateTime.now().minusDays(cleanupDays);
        
        try {
            long deletedCount = replicationLogRepository.count();
            replicationLogRepository.deleteByTimestampBefore(cutoff);
            long remainingCount = replicationLogRepository.count();
            
            logger.info("Cleaned up {} old replication logs, {} remaining", 
                deletedCount - remainingCount, remainingCount);
                
        } catch (Exception e) {
            logger.error("Failed to cleanup old replication logs", e);
        }
    }
    
    public String getInstanceId() {
        return instanceId;
    }
    
    public boolean isReplicationEnabled() {
        return replicationEnabled;
    }
    
    @Transactional
    public List<ReplicationLog> getReplicationLogsSince(LocalDateTime since) {
        return replicationLogRepository.findLogsSince(since);
    }
    
    @Transactional
    public List<ReplicationLog> getAllUnprocessedLogs() {
        return replicationLogRepository.findUnprocessedLogs();
    }
}

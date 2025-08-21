package com.example.vault.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "replication_logs", indexes = {
    @Index(name = "idx_replication_timestamp", columnList = "timestamp"),
    @Index(name = "idx_replication_entity", columnList = "entityType,entityId")
})
@EntityListeners(AuditingEntityListener.class)
public class ReplicationLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntityType entityType;
    
    @Column(nullable = false)
    private Long entityId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationType operationType;
    
    @Lob
    @Column
    private String entityData;
    
    @Column(nullable = false)
    private String sourceInstance;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
    
    @Column(nullable = false)
    private Boolean processed = false;
    
    // Constructors
    public ReplicationLog() {}
    
    public ReplicationLog(EntityType entityType, Long entityId, OperationType operationType, 
                         String entityData, String sourceInstance) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.operationType = operationType;
        this.entityData = entityData;
        this.sourceInstance = sourceInstance;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public EntityType getEntityType() { return entityType; }
    public void setEntityType(EntityType entityType) { this.entityType = entityType; }
    
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    
    public OperationType getOperationType() { return operationType; }
    public void setOperationType(OperationType operationType) { this.operationType = operationType; }
    
    public String getEntityData() { return entityData; }
    public void setEntityData(String entityData) { this.entityData = entityData; }
    
    public String getSourceInstance() { return sourceInstance; }
    public void setSourceInstance(String sourceInstance) { this.sourceInstance = sourceInstance; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public Boolean getProcessed() { return processed; }
    public void setProcessed(Boolean processed) { this.processed = processed; }
    
    public enum EntityType {
        SECRET, IDENTITY, POLICY
    }
    
    public enum OperationType {
        CREATE, UPDATE, DELETE
    }
}

package com.example.vault.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "secrets", indexes = {
    @Index(name = "idx_secret_path", columnList = "path"),
    @Index(name = "idx_secret_path_key", columnList = "path,secret_key"),
    @Index(name = "idx_secret_version", columnList = "path,secret_key,version"),
    @Index(name = "idx_secret_latest", columnList = "path,secret_key,deleted,version")
})
@EntityListeners(AuditingEntityListener.class)
public class Secret {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(max = 500)
    @Column(nullable = false)
    private String path;
    
    @Column(nullable = false)
    private Integer version = 1;
    
    @Column(name = "secret_key", nullable = false)
    private String key;
    
    @Lob
    @Column(name = "encrypted_value", nullable = false)
    private String encryptedValue;
    
    @Column(nullable = false)
    private Boolean deleted = false;
    
    @Column
    private LocalDateTime deletedAt;
    
    @Size(max = 1000)
    @Column
    private String metadata;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_identity_id")
    private Identity createdBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_identity_id")
    private Identity updatedBy;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    public Secret() {}
    
    public Secret(String path, String key, String encryptedValue, Identity createdBy) {
        this.path = path;
        this.key = key;
        this.encryptedValue = encryptedValue;
        this.createdBy = createdBy;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    
    public String getEncryptedValue() { return encryptedValue; }
    public void setEncryptedValue(String encryptedValue) { this.encryptedValue = encryptedValue; }
    
    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
    
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    
    public Identity getCreatedBy() { return createdBy; }
    public void setCreatedBy(Identity createdBy) { this.createdBy = createdBy; }
    
    public Identity getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Identity updatedBy) { this.updatedBy = updatedBy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    /**
     * Get the full path including key for hierarchical storage
     */
    public String getFullPath() {
        return path + "/" + key;
    }
    
    /**
     * Check if this path is a parent of another path
     */
    public boolean isParentOf(String childPath) {
        return childPath.startsWith(this.path + "/");
    }
    
    /**
     * Get the unique identifier combining path and key
     */
    public String getSecretIdentifier() {
        return path + "/" + key;
    }
    
    /**
     * Get the version identifier combining path, key, and version
     */
    public String getVersionIdentifier() {
        return path + "/" + key + "@v" + version;
    }
    
    @Override
    public String toString() {
        return "Secret{" +
                "id=" + id +
                ", path='" + path + '\'' +
                ", key='" + key + '\'' +
                ", version=" + version +
                ", deleted=" + deleted +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

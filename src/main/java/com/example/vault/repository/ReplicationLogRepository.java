package com.example.vault.repository;

import com.example.vault.entity.ReplicationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReplicationLogRepository extends JpaRepository<ReplicationLog, Long> {
    
    @Query("SELECT r FROM ReplicationLog r WHERE r.processed = false ORDER BY r.timestamp ASC")
    List<ReplicationLog> findUnprocessedLogs();
    
    @Query("SELECT r FROM ReplicationLog r WHERE r.processed = false AND r.sourceInstance != :currentInstance ORDER BY r.timestamp ASC")
    List<ReplicationLog> findUnprocessedLogsFromOtherInstances(@Param("currentInstance") String currentInstance);
    
    @Query("SELECT r FROM ReplicationLog r WHERE r.timestamp > :since ORDER BY r.timestamp ASC")
    List<ReplicationLog> findLogsSince(@Param("since") LocalDateTime since);
    
    void deleteByTimestampBefore(LocalDateTime cutoff);
}

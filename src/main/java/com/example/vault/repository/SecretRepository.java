package com.example.vault.repository;

import com.example.vault.entity.Secret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SecretRepository extends JpaRepository<Secret, Long> {
    
    @Query("SELECT s FROM Secret s WHERE s.path = :path AND s.key = :key AND s.deleted = false ORDER BY s.version DESC LIMIT 1")
    Optional<Secret> findByPathAndKeyAndDeletedFalse(@Param("path") String path, @Param("key") String key);
    
    @Query("SELECT s FROM Secret s WHERE s.path = :path AND s.key = :key AND s.version = :version AND s.deleted = false")
    Optional<Secret> findByPathAndKeyAndVersion(@Param("path") String path, @Param("key") String key, @Param("version") Integer version);
    
    @Query("SELECT s FROM Secret s WHERE s.path = :path AND s.key = :key AND s.version = :version")
    Optional<Secret> findByPathAndKeyAndVersionIncludingDeleted(@Param("path") String path, @Param("key") String key, @Param("version") Integer version);
    
    @Query("SELECT s FROM Secret s WHERE s.path = :path AND s.deleted = false AND s.version = (SELECT MAX(s2.version) FROM Secret s2 WHERE s2.path = s.path AND s2.key = s.key) ORDER BY s.key")
    List<Secret> findByPathAndDeletedFalse(@Param("path") String path);
    
    @Query("SELECT DISTINCT s.path FROM Secret s WHERE s.path LIKE :pathPrefix% AND s.deleted = false")
    List<String> findPathsByPrefix(@Param("pathPrefix") String pathPrefix);
    
    @Query("SELECT s FROM Secret s WHERE s.path LIKE :pathPrefix% AND s.deleted = false AND s.version = (SELECT MAX(s2.version) FROM Secret s2 WHERE s2.path = s.path AND s2.key = s.key) ORDER BY s.path, s.key")
    List<Secret> findByPathPrefixAndDeletedFalse(@Param("pathPrefix") String pathPrefix);
    
    @Query("SELECT MAX(s.version) FROM Secret s WHERE s.path = :path AND s.key = :key")
    Integer findMaxVersionByPathAndKey(@Param("path") String path, @Param("key") String key);
    
    @Query("SELECT s FROM Secret s WHERE s.path = :path AND s.key = :key")
    List<Secret> findAllByPathAndKey(@Param("path") String path, @Param("key") String key);
    
    @Query("SELECT s FROM Secret s WHERE s.path = :path AND s.key = :key AND s.deleted = false ORDER BY s.version DESC")
    List<Secret> findAllVersionsByPathAndKey(@Param("path") String path, @Param("key") String key);
    
    @Query("SELECT s FROM Secret s WHERE s.path = :path AND s.key = :key AND s.deleted = false ORDER BY s.version ASC")
    List<Secret> findAllVersionsByPathAndKeyAsc(@Param("path") String path, @Param("key") String key);
    
    @Query("SELECT COUNT(s) FROM Secret s WHERE s.path = :path AND s.key = :key AND s.deleted = false")
    Long countVersionsByPathAndKey(@Param("path") String path, @Param("key") String key);
    
    @Query("SELECT MIN(s.version) FROM Secret s WHERE s.path = :path AND s.key = :key AND s.deleted = false")
    Integer findMinVersionByPathAndKey(@Param("path") String path, @Param("key") String key);
    
    @Query("SELECT DISTINCT s.path, s.key, MAX(s.version) as maxVersion, COUNT(s.version) as versionCount " +
           "FROM Secret s WHERE s.path = :path AND s.deleted = false " +
           "GROUP BY s.path, s.key ORDER BY s.key")
    List<Object[]> findSecretSummariesByPath(@Param("path") String path);
    
    @Query("SELECT s FROM Secret s WHERE s.path = :path AND s.key = :key AND s.version BETWEEN :startVersion AND :endVersion AND s.deleted = false ORDER BY s.version DESC")
    List<Secret> findVersionRangeByPathAndKey(@Param("path") String path, @Param("key") String key, 
                                            @Param("startVersion") Integer startVersion, 
                                            @Param("endVersion") Integer endVersion);
    
    boolean existsByPathAndKeyAndDeletedFalse(String path, String key);
}

package com.example.vault.repository;

import com.example.vault.entity.Identity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdentityRepository extends JpaRepository<Identity, Long> {
    
    Optional<Identity> findByName(String name);
    
    Optional<Identity> findByNameAndEnabled(String name, Boolean enabled);
    
    @Query("SELECT i FROM Identity i JOIN FETCH i.policies WHERE i.name = :name AND i.enabled = true")
    Optional<Identity> findByNameWithPolicies(@Param("name") String name);
    
    boolean existsByName(String name);
}

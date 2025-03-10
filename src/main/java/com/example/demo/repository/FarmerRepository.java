package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Farmer;

@Repository
public interface FarmerRepository extends JpaRepository<Farmer, Long> {
    
    Optional<Farmer> findByUniqueId(String uniqueId);
    
    boolean existsByUidNumber(String uidNumber);
    
    @Query("SELECT f FROM Farmer f WHERE f.adminDeleted = false")
    List<Farmer> findAllActive();
}

package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Consumer;

@Repository
public interface ConsumerRepository extends JpaRepository<Consumer, Long> {
    
    Optional<Consumer> findByUniqueId(String uniqueId);
    
    @Query("SELECT c FROM Consumer c WHERE c.adminDeleted = false")
    List<Consumer> findAllActive();
    
    @Query("SELECT c FROM Consumer c WHERE c.email = ?1 OR c.username = ?1 OR c.phoneNumber = ?1")
    Optional<Consumer> findByEmailOrUsernameOrPhoneNumber(String identifier);
}

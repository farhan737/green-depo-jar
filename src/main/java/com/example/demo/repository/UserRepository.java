package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByUniqueId(String uniqueId);
    boolean existsByUsername(String username);
    
    Optional<User> findByUsernameOrUniqueIdOrPhoneNumber(String username, String uniqueId, String phoneNumber);
    Optional<User> findByUsername(String username);
    Optional<User> findByUniqueId(String uniqueId);
    Optional<User> findByPhoneNumber(String phoneNumber);
    boolean existsByUsernameAndUserType(String username, String userType);
}

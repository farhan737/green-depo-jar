package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.PriceLimit;

@Repository
public interface PriceLimitRepository extends JpaRepository<PriceLimit, Long> {
    
    Optional<PriceLimit> findByProductTypeAndProductName(String productType, String productName);
    
    boolean existsByProductTypeAndProductName(String productType, String productName);
}

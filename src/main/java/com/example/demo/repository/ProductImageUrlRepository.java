package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.ProductImageUrl;

@Repository
public interface ProductImageUrlRepository extends JpaRepository<ProductImageUrl, Long> {
    // Add custom query methods if needed
}

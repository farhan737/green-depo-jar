package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Farmer;
import com.example.demo.model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    List<Product> findByFarmer(Farmer farmer);
    
    @Query("SELECT p FROM Product p WHERE p.farmer.uniqueId = :farmerId")
    List<Product> findByFarmerUniqueId(@Param("farmerId") String farmerId);
    
    Optional<Product> findByProductId(String productId);
    
    boolean existsByProductId(String productId);
    
    @Query("SELECT p FROM Product p WHERE p.adminDeleted = false")
    List<Product> findAllActive();
    
    @Query("SELECT p FROM Product p WHERE p.farmer = :farmer AND p.adminDeleted = false")
    List<Product> findByFarmerAndNotDeleted(@Param("farmer") Farmer farmer);
    
    @Query("SELECT p FROM Product p WHERE p.farmer.uniqueId = :farmerId AND p.adminDeleted = false")
    List<Product> findByFarmerUniqueIdAndNotDeleted(@Param("farmerId") String farmerId);
}

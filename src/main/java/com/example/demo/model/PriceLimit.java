package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "price_limits")
public class PriceLimit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String productType;
    
    @Column(nullable = false)
    private String productName;
    
    @Column(nullable = false)
    private Double pricePerGram;
    
    @Column(nullable = false)
    private Double pricePerKg;
    
    // Constructors
    public PriceLimit() {
    }
    
    public PriceLimit(String productType, String productName, Double pricePerGram, Double pricePerKg) {
        this.productType = productType;
        this.productName = productName;
        this.pricePerGram = pricePerGram;
        this.pricePerKg = pricePerKg;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getProductType() {
        return productType;
    }
    
    public void setProductType(String productType) {
        this.productType = productType;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public Double getPricePerGram() {
        return pricePerGram;
    }
    
    public void setPricePerGram(Double pricePerGram) {
        this.pricePerGram = pricePerGram;
    }
    
    public Double getPricePerKg() {
        return pricePerKg;
    }
    
    public void setPricePerKg(Double pricePerKg) {
        this.pricePerKg = pricePerKg;
    }
    
    // Helper method to get price limit based on weight unit
    public Double getPriceLimit(String weightUnit, Double weight) {
        if ("g".equals(weightUnit)) {
            return pricePerGram * weight;
        } else if ("kg".equals(weightUnit)) {
            return pricePerKg * weight;
        }
        return null;
    }
}

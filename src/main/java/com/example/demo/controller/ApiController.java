package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.PriceLimitService;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private PriceLimitService priceLimitService;
    
    /**
     * API endpoint to get the maximum price for a product based on type, name, weight unit, and weight
     * Used by the frontend to validate prices during product creation
     */
    @GetMapping("/price-limits")
    public ResponseEntity<Map<String, Object>> getPriceLimit(
            @RequestParam String productType,
            @RequestParam String productName,
            @RequestParam String weightUnit,
            @RequestParam Double weight) {
        
        Map<String, Object> response = new HashMap<>();
        
        // Calculate the maximum price based on the price limit
        Double maxPrice = priceLimitService.getMaxAllowedPrice(productType, productName, weightUnit, weight);
        
        if (maxPrice != null) {
            response.put("maxPrice", maxPrice);
            response.put("productType", productType);
            response.put("productName", productName);
            response.put("weightUnit", weightUnit);
            response.put("weight", weight);
        } else {
            response.put("maxPrice", null);
            response.put("message", "No price limit found for this product");
        }
        
        return ResponseEntity.ok(response);
    }
}

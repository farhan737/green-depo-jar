package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.PriceLimit;
import com.example.demo.repository.PriceLimitRepository;

@Service
public class PriceLimitService {
    
    private static final Logger logger = LoggerFactory.getLogger(PriceLimitService.class);
    
    @Autowired
    private PriceLimitRepository priceLimitRepository;
    
    /**
     * Get all price limits
     * @return List of all price limits
     */
    public List<PriceLimit> getAllPriceLimits() {
        return priceLimitRepository.findAll();
    }
    
    /**
     * Get price limit by product type and name
     * @param productType The product type
     * @param productName The product name
     * @return Optional containing the price limit if found
     */
    public Optional<PriceLimit> getPriceLimit(String productType, String productName) {
        return priceLimitRepository.findByProductTypeAndProductName(productType, productName);
    }
    
    /**
     * Create or update a price limit
     * @param priceLimit The price limit to save
     * @return The saved price limit
     */
    @Transactional
    public PriceLimit savePriceLimit(PriceLimit priceLimit) {
        logger.info("Saving price limit for {}/{}: {} per gram, {} per kg", 
                priceLimit.getProductType(), priceLimit.getProductName(),
                priceLimit.getPricePerGram(), priceLimit.getPricePerKg());
        return priceLimitRepository.save(priceLimit);
    }
    
    /**
     * Create or update multiple price limits
     * @param priceLimits List of price limits to save
     * @return List of saved price limits
     */
    @Transactional
    public List<PriceLimit> saveAllPriceLimits(List<PriceLimit> priceLimits) {
        logger.info("Saving {} price limits", priceLimits.size());
        return priceLimitRepository.saveAll(priceLimits);
    }
    
    /**
     * Delete a price limit
     * @param id The ID of the price limit to delete
     */
    @Transactional
    public void deletePriceLimit(Long id) {
        logger.info("Deleting price limit with ID: {}", id);
        priceLimitRepository.deleteById(id);
    }
    
    /**
     * Check if a product's price exceeds the limit
     * @param productType The product type
     * @param productName The product name
     * @param weightUnit The weight unit (g or kg)
     * @param weight The weight amount
     * @param price The price to check
     * @return true if price exceeds limit, false otherwise
     */
    public boolean isPriceExceedingLimit(String productType, String productName, String weightUnit, Double weight, Double price) {
        Optional<PriceLimit> limitOpt = getPriceLimit(productType, productName);
        
        if (limitOpt.isPresent()) {
            PriceLimit limit = limitOpt.get();
            Double maxPrice = limit.getPriceLimit(weightUnit, weight);
            
            if (maxPrice != null && price > maxPrice) {
                logger.info("Price {} exceeds limit {} for {}/{} with weight {} {}", 
                        price, maxPrice, productType, productName, weight, weightUnit);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get the maximum allowed price for a product
     * @param productType The product type
     * @param productName The product name
     * @param weightUnit The weight unit (g or kg)
     * @param weight The weight amount
     * @return The maximum allowed price, or null if no limit exists
     */
    public Double getMaxAllowedPrice(String productType, String productName, String weightUnit, Double weight) {
        Optional<PriceLimit> limitOpt = getPriceLimit(productType, productName);
        
        if (limitOpt.isPresent()) {
            PriceLimit limit = limitOpt.get();
            return limit.getPriceLimit(weightUnit, weight);
        }
        
        return null;
    }
}

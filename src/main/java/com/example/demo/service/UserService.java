package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.model.Consumer;
import com.example.demo.model.Farmer;
import com.example.demo.model.Product;
import com.example.demo.model.User;
import com.example.demo.repository.ConsumerRepository;
import com.example.demo.repository.FarmerRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.UniqueIdGenerator;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private FarmerRepository farmerRepository;
    
    @Autowired
    private ConsumerRepository consumerRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private UniqueIdGenerator uniqueIdGenerator;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public boolean isEmailTaken(String email) {
        return userRepository.existsByEmail(email);
    }
    
    public boolean isPhoneNumberTaken(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }
    
    public boolean isUidNumberTaken(String uidNumber) {
        return farmerRepository.existsByUidNumber(uidNumber);
    }
    
    public boolean isUniqueIdTaken(String uniqueId) {
        return userRepository.existsByUniqueId(uniqueId);
    }
    
    public boolean isUsernameTaken(String username) {
        return userRepository.existsByUsername(username);
    }
    
    public String generateUniqueId() {
        String uniqueId;
        do {
            uniqueId = uniqueIdGenerator.generateUniqueId();
        } while (isUniqueIdTaken(uniqueId));
        
        return uniqueId;
    }
    
    public Farmer registerFarmer(Farmer farmer) {
        // Set unique ID before saving
        farmer.setUniqueId(generateUniqueId());
        
        // Log original password (for debugging only, remove in production)
        String originalPassword = farmer.getPassword();
        logger.debug("Original password before encoding: {}", originalPassword);
        
        // Encode password
        String encodedPassword = passwordEncoder.encode(originalPassword);
        farmer.setPassword(encodedPassword);
        logger.debug("Password after encoding: {}", encodedPassword);
        
        Farmer savedFarmer = farmerRepository.save(farmer);
        logger.info("Farmer registered successfully with username: {}, uniqueId: {}", 
                savedFarmer.getUsername(), savedFarmer.getUniqueId());
        
        return savedFarmer;
    }
    
    public Consumer registerConsumer(Consumer consumer) {
        // Set unique ID before saving
        consumer.setUniqueId(generateUniqueId());
        
        // Log original password (for debugging only, remove in production)
        String originalPassword = consumer.getPassword();
        logger.debug("Original password before encoding: {}", originalPassword);
        
        // Encode password
        String encodedPassword = passwordEncoder.encode(originalPassword);
        consumer.setPassword(encodedPassword);
        logger.debug("Password after encoding: {}", encodedPassword);
        
        Consumer savedConsumer = consumerRepository.save(consumer);
        logger.info("Consumer registered successfully with username: {}, uniqueId: {}", 
                savedConsumer.getUsername(), savedConsumer.getUniqueId());
        
        return savedConsumer;
    }
    
    public User registerAdmin(String username, String password) {
        User admin = new User();
        admin.setUniqueId(generateUniqueId());
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEmail("admin@greendepo.com");
        admin.setPhoneNumber("0000000000");
        admin.setUserType("ADMIN");
        admin.setState("Admin");
        admin.setDistrict("Admin");
        admin.setCity("Admin");
        admin.setLocationLink("Admin");
        admin.setDateOfBirth(java.time.LocalDate.now());
        
        User savedAdmin = userRepository.save(admin);
        logger.info("Admin registered successfully with username: {}, uniqueId: {}", 
                savedAdmin.getUsername(), savedAdmin.getUniqueId());
        
        return savedAdmin;
    }
    
    public User findUserByLoginIdentifier(String identifier) {
        Optional<User> userOpt = userRepository.findByUsernameOrUniqueIdOrPhoneNumber(identifier, identifier, identifier);
        return userOpt.orElse(null);
    }
    
    public List<Farmer> getAllFarmers() {
        return farmerRepository.findAllActive();
    }
    
    public List<Consumer> getAllConsumers() {
        return consumerRepository.findAllActive();
    }
    
    public Farmer getFarmerByUniqueId(String uniqueId) {
        return farmerRepository.findByUniqueId(uniqueId).orElse(null);
    }
    
    public Consumer getConsumerByUniqueId(String uniqueId) {
        return consumerRepository.findByUniqueId(uniqueId).orElse(null);
    }
    
    public void deleteFarmer(String uniqueId) {
        Farmer farmer = getFarmerByUniqueId(uniqueId);
        if (farmer != null) {
            // Mark farmer as deleted instead of physically deleting
            farmer.setAdminDeleted(true);
            farmerRepository.save(farmer);
            
            // Mark all products of this farmer as deleted
            markAllFarmerProductsAsDeleted(farmer);
            
            logger.info("Farmer marked as deleted with uniqueId: {}", uniqueId);
        } else {
            logger.warn("Attempted to delete non-existent farmer with uniqueId: {}", uniqueId);
            throw new IllegalArgumentException("Farmer not found with uniqueId: " + uniqueId);
        }
    }
    
    public void deleteConsumer(String uniqueId) {
        Consumer consumer = getConsumerByUniqueId(uniqueId);
        if (consumer != null) {
            // Mark consumer as deleted instead of physically deleting
            consumer.setAdminDeleted(true);
            consumerRepository.save(consumer);
            logger.info("Consumer marked as deleted with uniqueId: {}", uniqueId);
        } else {
            logger.warn("Attempted to delete non-existent consumer with uniqueId: {}", uniqueId);
            throw new IllegalArgumentException("Consumer not found with uniqueId: " + uniqueId);
        }
    }
    
    public void updateFarmerDetails(String uniqueId, String firstName, String lastName, String phoneNumber, String email) {
        Farmer farmer = getFarmerByUniqueId(uniqueId);
        if (farmer != null) {
            farmer.setFirstName(firstName);
            farmer.setLastName(lastName);
            farmer.setPhoneNumber(phoneNumber);
            farmer.setEmail(email);
            farmerRepository.save(farmer);
            logger.info("Farmer updated with uniqueId: {}", uniqueId);
        } else {
            logger.warn("Attempted to update non-existent farmer with uniqueId: {}", uniqueId);
            throw new IllegalArgumentException("Farmer not found with uniqueId: " + uniqueId);
        }
    }
    
    public void updateConsumerDetails(String uniqueId, String firstName, String lastName, String phoneNumber, String email) {
        Consumer consumer = getConsumerByUniqueId(uniqueId);
        if (consumer != null) {
            consumer.setFirstName(firstName);
            consumer.setLastName(lastName);
            consumer.setPhoneNumber(phoneNumber);
            consumer.setEmail(email);
            consumerRepository.save(consumer);
            logger.info("Consumer updated with uniqueId: {}", uniqueId);
        } else {
            logger.warn("Attempted to update non-existent consumer with uniqueId: {}", uniqueId);
            throw new IllegalArgumentException("Consumer not found with uniqueId: " + uniqueId);
        }
    }
    
    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }
    
    public User findUserByUniqueId(String uniqueId) {
        return userRepository.findByUniqueId(uniqueId).orElse(null);
    }
    
    public boolean isUsernameExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }
    
    public boolean isUniqueIdExists(String uniqueId) {
        return userRepository.findByUniqueId(uniqueId).isPresent();
    }
    
    public boolean isPhoneNumberExists(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber).isPresent();
    }
    
    /**
     * Marks all products of a farmer as deleted by admin
     * @param farmer The farmer whose products should be marked as deleted
     */
    private void markAllFarmerProductsAsDeleted(Farmer farmer) {
        List<Product> farmerProducts = productRepository.findByFarmer(farmer);
        for (Product product : farmerProducts) {
            product.setAdminDeleted(true);
            product.setActive(false);
        }
        if (!farmerProducts.isEmpty()) {
            productRepository.saveAll(farmerProducts);
            logger.info("Marked {} products as deleted for farmer with uniqueId: {}", 
                    farmerProducts.size(), farmer.getUniqueId());
        }
    }
    
    /**
     * Restores a previously deleted farmer by setting adminDeleted flag to false
     * @param uniqueId The unique ID of the farmer to restore
     */
    public void restoreFarmer(String uniqueId) {
        Farmer farmer = getFarmerByUniqueId(uniqueId);
        if (farmer != null) {
            // Mark farmer as not deleted
            farmer.setAdminDeleted(false);
            farmerRepository.save(farmer);
            
            // Restore all products of this farmer
            restoreAllFarmerProducts(farmer);
            
            logger.info("Farmer restored with uniqueId: {}", uniqueId);
        } else {
            logger.warn("Attempted to restore non-existent farmer with uniqueId: {}", uniqueId);
            throw new IllegalArgumentException("Farmer not found with uniqueId: " + uniqueId);
        }
    }
    
    /**
     * Restores a previously deleted consumer by setting adminDeleted flag to false
     * @param uniqueId The unique ID of the consumer to restore
     */
    public void restoreConsumer(String uniqueId) {
        Consumer consumer = getConsumerByUniqueId(uniqueId);
        if (consumer != null) {
            // Mark consumer as not deleted
            consumer.setAdminDeleted(false);
            consumerRepository.save(consumer);
            logger.info("Consumer restored with uniqueId: {}", uniqueId);
        } else {
            logger.warn("Attempted to restore non-existent consumer with uniqueId: {}", uniqueId);
            throw new IllegalArgumentException("Consumer not found with uniqueId: " + uniqueId);
        }
    }
    
    /**
     * Marks all products of a farmer as not deleted by admin
     * @param farmer The farmer whose products should be restored
     */
    private void restoreAllFarmerProducts(Farmer farmer) {
        List<Product> farmerProducts = productRepository.findByFarmer(farmer);
        for (Product product : farmerProducts) {
            if (product.getAdminDeleted()) {
                product.setAdminDeleted(false);
                // Only set active to true if it was previously active before deletion
                // This preserves the original active state
                if (product.getAvailableStock() > 0) {
                    product.setActive(true);
                }
            }
        }
        if (!farmerProducts.isEmpty()) {
            productRepository.saveAll(farmerProducts);
            logger.info("Restored {} products for farmer with uniqueId: {}", 
                    farmerProducts.size(), farmer.getUniqueId());
        }
    }
}

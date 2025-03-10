package com.example.demo.service;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.Farmer;
import com.example.demo.model.Product;
import com.example.demo.model.ProductImage;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.util.ProductIdGenerator;

@Service
public class ProductService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private ProductIdGenerator productIdGenerator;
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    @Transactional
    public Product createProduct(Product product, Farmer farmer, List<MultipartFile> imageFiles) throws IOException {
        // Generate a unique product ID
        String productId;
        do {
            productId = productIdGenerator.generateProductId();
        } while (productRepository.existsByProductId(productId));
        
        product.setProductId(productId);
        product.setFarmer(farmer);
        
        // Save the product first to get an ID
        Product savedProduct = productRepository.save(product);
        logger.info("Product created successfully with ID: {}", savedProduct.getProductId());
        
        // Process and save images
        if (imageFiles != null && !imageFiles.isEmpty()) {
            int order = 0;
            for (MultipartFile file : imageFiles) {
                if (!file.isEmpty()) {
                    ProductImage image = new ProductImage(
                        file.getOriginalFilename(),
                        file.getContentType(),
                        file.getBytes(),
                        order++
                    );
                    savedProduct.addImage(image);
                }
            }
            // Save the product again with images
            savedProduct = productRepository.save(savedProduct);
            logger.info("Added {} images to product {}", order, savedProduct.getProductId());
        }
        
        return savedProduct;
    }
    
    public List<Product> getProductsByFarmer(Farmer farmer) {
        return productRepository.findByFarmerAndNotDeleted(farmer);
    }
    
    /**
     * Get all products by farmer ID
     * @param farmerId The unique ID of the farmer
     * @return List of products belonging to the farmer
     */
    public List<Product> getProductsByFarmerId(String farmerId) {
        logger.info("Fetching products for farmer with ID: {}", farmerId);
        return productRepository.findByFarmerUniqueIdAndNotDeleted(farmerId);
    }
    
    public List<Product> getAllProducts() {
        return productRepository.findAllActive();
    }
    
    public Product getProductByProductId(String productId) {
        return productRepository.findByProductId(productId).orElse(null);
    }
    
    public Product getProductById(String productId) {
        return productRepository.findByProductId(productId).orElse(null);
    }
    
    @Transactional
    public void deleteProduct(String productId) {
        Product product = getProductById(productId);
        if (product != null) {
            // Check if product is referenced in any order items
            if (orderItemRepository.existsByProduct(product)) {
                logger.warn("Cannot delete product with ID: {} because it is referenced in order items", productId);
                throw new IllegalStateException("Cannot delete product because it is referenced in orders. Consider marking it as unavailable instead.");
            }
            
            // Mark product as deleted instead of physically deleting
            product.setAdminDeleted(true);
            product.setActive(false);
            productRepository.save(product);
            logger.info("Product marked as deleted with productId: {}", productId);
        } else {
            logger.warn("Attempted to delete non-existent product with productId: {}", productId);
            throw new IllegalArgumentException("Product not found with productId: " + productId);
        }
    }
    
    @Transactional
    public void deleteProduct(Product product) {
        // Check if product is referenced in any order items
        if (orderItemRepository.existsByProduct(product)) {
            logger.warn("Cannot delete product with ID: {} because it is referenced in order items", product.getProductId());
            throw new IllegalStateException("Cannot delete product because it is referenced in orders. Consider marking it as unavailable instead.");
        }
        
        // Mark product as deleted instead of physically deleting
        product.setAdminDeleted(true);
        product.setActive(false);
        productRepository.save(product);
        logger.info("Product marked as deleted with ID: {}", product.getProductId());
    }
    
    @Transactional
    public void updateProductDetails(String productId, String title, String description, double price, double weight) {
        Product product = getProductById(productId);
        if (product != null) {
            product.setTitle(title);
            product.setDescription(description);
            product.setPrice(price);
            product.setWeight(weight);
            productRepository.save(product);
            logger.info("Product updated with productId: {}", productId);
        } else {
            logger.warn("Attempted to update non-existent product with productId: {}", productId);
            throw new IllegalArgumentException("Product not found with productId: " + productId);
        }
    }
    
    @Transactional
    public Product updateProduct(Product product, List<MultipartFile> newImageFiles) throws IOException {
        // Check if we have new images
        if (newImageFiles != null && !newImageFiles.isEmpty()) {
            // Remove existing images if we're replacing them
            boolean hasNonEmptyFile = newImageFiles.stream().anyMatch(file -> !file.isEmpty());
            
            if (hasNonEmptyFile) {
                // Clear existing images
                product.getImages().clear();
                
                // Add new images
                int order = 0;
                for (MultipartFile file : newImageFiles) {
                    if (!file.isEmpty()) {
                        ProductImage image = new ProductImage(
                            file.getOriginalFilename(),
                            file.getContentType(),
                            file.getBytes(),
                            order++
                        );
                        product.addImage(image);
                    }
                }
            }
        }
        
        Product updatedProduct = productRepository.save(product);
        logger.info("Product updated successfully with ID: {}", updatedProduct.getProductId());
        return updatedProduct;
    }
    
    @Transactional
    public void markProductAsUnavailable(String productId) {
        Product product = getProductById(productId);
        if (product != null) {
            product.setAvailableStock(0);
            product.setActive(false);
            productRepository.save(product);
            logger.info("Product marked as unavailable with productId: {}", productId);
        } else {
            logger.warn("Attempted to mark non-existent product as unavailable with productId: {}", productId);
            throw new IllegalArgumentException("Product not found with productId: " + productId);
        }
    }
    
    public boolean isProductDeleted(String productId) {
        Product product = getProductByProductId(productId);
        if (product == null) {
            return true;
        }
        return product.getAdminDeleted();
    }
    
    public boolean isProductDeleted(Product product) {
        if (product == null) {
            return true;
        }
        return product.getAdminDeleted();
    }
    
    /**
     * Restores a previously deleted product by setting adminDeleted flag to false
     * @param productId The ID of the product to restore
     */
    @Transactional
    public void restoreProduct(String productId) {
        Product product = getProductByProductId(productId);
        if (product != null) {
            product.setAdminDeleted(false);
            
            // Only set active to true if it has stock available
            if (product.getAvailableStock() > 0) {
                product.setActive(true);
            }
            
            productRepository.save(product);
            logger.info("Product restored with productId: {}", productId);
        } else {
            logger.warn("Attempted to restore non-existent product with productId: {}", productId);
            throw new IllegalArgumentException("Product not found with productId: " + productId);
        }
    }
    
    /**
     * Restores a previously deleted product by setting adminDeleted flag to false
     * @param product The product to restore
     */
    @Transactional
    public void restoreProduct(Product product) {
        if (product != null) {
            product.setAdminDeleted(false);
            
            // Only set active to true if it has stock available
            if (product.getAvailableStock() > 0) {
                product.setActive(true);
            }
            
            productRepository.save(product);
            logger.info("Product restored with ID: {}", product.getProductId());
        } else {
            throw new IllegalArgumentException("Cannot restore null product");
        }
    }
}

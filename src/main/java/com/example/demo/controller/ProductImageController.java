package com.example.demo.controller;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.example.demo.model.Product;
import com.example.demo.model.ProductImage;
import com.example.demo.repository.ProductImageRepository;
import com.example.demo.repository.ProductRepository;

@Controller
public class ProductImageController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductImageController.class);

    @Autowired
    private ProductImageRepository productImageRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @GetMapping("/images/product/{id}")
    public ResponseEntity<byte[]> getProductImageForFarmer(@PathVariable Long id) {
        return getProductImageById(id);
    }
    
    @GetMapping("/product-image/{id}")
    public ResponseEntity<byte[]> getProductImageForConsumer(@PathVariable Long id) {
        return getProductImageById(id);
    }
    
    @GetMapping("/product-image/{productId}/{index}")
    public ResponseEntity<byte[]> getProductImageByIndex(
            @PathVariable String productId, 
            @PathVariable Integer index) {
        
        logger.info("Fetching image for product ID: {} at index: {}", productId, index);
        
        try {
            // Find the product by productId
            Optional<Product> productOptional = productRepository.findByProductId(productId);
            
            if (!productOptional.isPresent()) {
                logger.error("Product not found with ID: {}", productId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            
            Product product = productOptional.get();
            List<ProductImage> images = product.getImages();
            
            logger.info("Found product: {}, with {} images", product.getTitle(), images.size());
            
            // Check if the index is valid
            if (images == null || images.isEmpty()) {
                logger.error("No images found for product: {}", productId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            
            if (index < 0 || index >= images.size()) {
                logger.error("Invalid image index: {} for product: {} with {} images", 
                        index, productId, images.size());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            
            ProductImage image = images.get(index);
            logger.info("Found image: {} of type: {}", image.getFileName(), image.getContentType());
            
            HttpHeaders headers = new HttpHeaders();
            
            // Set content type based on the stored content type
            headers.setContentType(MediaType.parseMediaType(image.getContentType()));
            
            // Set filename for download
            headers.setContentDispositionFormData("inline", image.getFileName());
            
            return new ResponseEntity<>(image.getData(), headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving image for product ID: {} at index: {}", productId, index, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/debug/product/{productId}")
    public ResponseEntity<String> debugProduct(@PathVariable String productId) {
        logger.info("Debugging product with ID: {}", productId);
        
        Optional<Product> productOptional = productRepository.findByProductId(productId);
        
        if (!productOptional.isPresent()) {
            logger.error("Product not found with ID: {}", productId);
            return new ResponseEntity<>("Product not found with ID: " + productId, HttpStatus.NOT_FOUND);
        }
        
        Product product = productOptional.get();
        List<ProductImage> images = product.getImages();
        
        StringBuilder sb = new StringBuilder();
        sb.append("Product ID: ").append(product.getProductId()).append("\n");
        sb.append("Title: ").append(product.getTitle()).append("\n");
        sb.append("Number of images: ").append(images.size()).append("\n");
        
        for (int i = 0; i < images.size(); i++) {
            ProductImage image = images.get(i);
            sb.append("Image ").append(i).append(": ")
              .append("ID=").append(image.getId())
              .append(", Filename=").append(image.getFileName())
              .append(", ContentType=").append(image.getContentType())
              .append(", DisplayOrder=").append(image.getDisplayOrder())
              .append(", Data length=").append(image.getData().length)
              .append("\n");
        }
        
        return new ResponseEntity<>(sb.toString(), HttpStatus.OK);
    }
    
    private ResponseEntity<byte[]> getProductImageById(Long id) {
        logger.info("Fetching image by ID: {}", id);
        
        try {
            Optional<ProductImage> imageOptional = productImageRepository.findById(id);
            
            if (!imageOptional.isPresent()) {
                logger.error("Image not found with ID: {}", id);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            
            ProductImage image = imageOptional.get();
            logger.info("Found image: {} of type: {}", image.getFileName(), image.getContentType());
            
            HttpHeaders headers = new HttpHeaders();
            
            // Set content type based on the stored content type
            headers.setContentType(MediaType.parseMediaType(image.getContentType()));
            
            // Set filename for download
            headers.setContentDispositionFormData("inline", image.getFileName());
            
            return new ResponseEntity<>(image.getData(), headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving image with ID: {}", id, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

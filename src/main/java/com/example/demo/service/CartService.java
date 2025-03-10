package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.CartItem;
import com.example.demo.model.Consumer;
import com.example.demo.model.Product;
import com.example.demo.repository.CartItemRepository;
import com.example.demo.repository.ProductRepository;

@Service
public class CartService {
    
    private static final Logger logger = LoggerFactory.getLogger(CartService.class);
    
    @Autowired
    private CartItemRepository cartItemRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    /**
     * Get all cart items for a consumer
     */
    public List<CartItem> getCartItems(Consumer consumer) {
        return cartItemRepository.findByConsumerId(consumer.getId());
    }
    
    /**
     * Add a product to the cart
     */
    @Transactional
    public CartItem addToCart(Consumer consumer, String productId, Integer quantity) {
        Optional<Product> productOptional = productRepository.findByProductId(productId);
        
        if (!productOptional.isPresent()) {
            logger.error("Product not found with ID: {}", productId);
            throw new IllegalArgumentException("Product not found");
        }
        
        Product product = productOptional.get();
        
        // Check if the requested quantity is valid
        if (quantity <= 0) {
            logger.error("Invalid quantity: {}", quantity);
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        
        // Check if the requested quantity is within the purchase limit
        if (product.getPurchaseLimit() > 0 && quantity > product.getPurchaseLimit()) {
            logger.error("Quantity {} exceeds purchase limit {}", quantity, product.getPurchaseLimit());
            throw new IllegalArgumentException("Quantity exceeds purchase limit");
        }
        
        // Check if the requested quantity is available in stock
        if (quantity > product.getAvailableStock()) {
            logger.error("Quantity {} exceeds available stock {}", quantity, product.getAvailableStock());
            throw new IllegalArgumentException("Quantity exceeds available stock");
        }
        
        // Check if the product is already in the cart
        Optional<CartItem> existingItem = cartItemRepository.findByConsumerAndProduct(consumer, product);
        
        if (existingItem.isPresent()) {
            // Update the quantity
            CartItem cartItem = existingItem.get();
            int newQuantity = cartItem.getQuantity() + quantity;
            
            // Check if the new quantity is within limits
            if (product.getPurchaseLimit() > 0 && newQuantity > product.getPurchaseLimit()) {
                logger.error("Total quantity {} exceeds purchase limit {}", newQuantity, product.getPurchaseLimit());
                throw new IllegalArgumentException("Total quantity exceeds purchase limit");
            }
            
            if (newQuantity > product.getAvailableStock()) {
                logger.error("Total quantity {} exceeds available stock {}", newQuantity, product.getAvailableStock());
                throw new IllegalArgumentException("Total quantity exceeds available stock");
            }
            
            cartItem.setQuantity(newQuantity);
            return cartItemRepository.save(cartItem);
        } else {
            // Create a new cart item
            CartItem cartItem = new CartItem(consumer, product, quantity);
            return cartItemRepository.save(cartItem);
        }
    }
    
    /**
     * Update the quantity of a cart item
     */
    @Transactional
    public CartItem updateCartItemQuantity(Consumer consumer, Long cartItemId, Integer quantity) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));
        
        // Verify the cart item belongs to the consumer
        if (!cartItem.getConsumer().getId().equals(consumer.getId())) {
            logger.error("Cart item {} does not belong to consumer {}", cartItemId, consumer.getId());
            throw new IllegalArgumentException("Cart item does not belong to this consumer");
        }
        
        Product product = cartItem.getProduct();
        
        // Check if the requested quantity is valid
        if (quantity <= 0) {
            logger.error("Invalid quantity: {}", quantity);
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        
        // Check if the requested quantity is within the purchase limit
        if (product.getPurchaseLimit() > 0 && quantity > product.getPurchaseLimit()) {
            logger.error("Quantity {} exceeds purchase limit {}", quantity, product.getPurchaseLimit());
            throw new IllegalArgumentException("Quantity exceeds purchase limit");
        }
        
        // Check if the requested quantity is available in stock
        if (quantity > product.getAvailableStock()) {
            logger.error("Quantity {} exceeds available stock {}", quantity, product.getAvailableStock());
            throw new IllegalArgumentException("Quantity exceeds available stock");
        }
        
        cartItem.setQuantity(quantity);
        return cartItemRepository.save(cartItem);
    }
    
    /**
     * Remove a product from the cart
     */
    @Transactional
    public void removeFromCart(Consumer consumer, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));
        
        // Verify the cart item belongs to the consumer
        if (!cartItem.getConsumer().getId().equals(consumer.getId())) {
            logger.error("Cart item {} does not belong to consumer {}", cartItemId, consumer.getId());
            throw new IllegalArgumentException("Cart item does not belong to this consumer");
        }
        
        cartItemRepository.delete(cartItem);
    }
    
    /**
     * Calculate the total price of all items in the cart
     */
    public double calculateCartTotal(List<CartItem> cartItems) {
        return cartItems.stream()
                .mapToDouble(CartItem::getTotalPrice)
                .sum();
    }
    
    /**
     * Clear all items from a consumer's cart
     */
    @Transactional
    public void clearCart(Consumer consumer) {
        List<CartItem> cartItems = cartItemRepository.findByConsumerId(consumer.getId());
        
        if (cartItems != null && !cartItems.isEmpty()) {
            for (CartItem item : cartItems) {
                cartItemRepository.delete(item);
            }
            logger.info("Cleared cart for consumer: {}", consumer.getUniqueId());
        }
    }
}

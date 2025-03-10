package com.example.demo.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.CartItem;
import com.example.demo.model.Consumer;
import com.example.demo.model.Farmer;
import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;
import com.example.demo.model.Product;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;

@Service
public class OrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private static final double TAX_RATE = 0.12; // 12% tax
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private CartService cartService;
    
    /**
     * Create a new order from cart items
     */
    @Transactional
    public Order createOrderFromCart(Consumer consumer, List<CartItem> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }
        
        // Calculate total amount
        double totalAmount = cartItems.stream()
                .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity())
                .sum();
        
        // Calculate tax amount (12%)
        double taxAmount = totalAmount * TAX_RATE;
        
        // Calculate final amount
        double finalAmount = totalAmount + taxAmount;
        
        // Generate unique order number
        String orderNumber = generateOrderNumber();
        
        // Create order
        Order order = new Order(
                orderNumber,
                consumer,
                LocalDateTime.now(),
                "PENDING",
                totalAmount,
                taxAmount,
                finalAmount
        );
        
        // Save order
        order = orderRepository.save(order);
        
        // Create order items
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            String paymentMethod = product.getPaymentOption();
            // Always set payment status to PENDING regardless of payment method
            String paymentStatus = "PENDING";
            
            OrderItem orderItem = new OrderItem(
                    order,
                    product,
                    cartItem.getQuantity(),
                    product.getPrice(),
                    product.getPrice() * cartItem.getQuantity(),
                    paymentMethod,
                    paymentStatus
            );
            
            order.addOrderItem(orderItem);
            
            // Update product stock
            int newStock = product.getAvailableStock() - cartItem.getQuantity();
            product.setAvailableStock(newStock);
            productRepository.save(product);
        }
        
        // Save order with items
        order = orderRepository.save(order);
        
        // Clear cart
        cartService.clearCart(consumer);
        
        return order;
    }
    
    /**
     * Process payment for pre-pickup items
     */
    @Transactional
    public void processPayment(String orderNumber) {
        Optional<Order> orderOptional = orderRepository.findByOrderNumber(orderNumber);
        
        if (!orderOptional.isPresent()) {
            throw new IllegalArgumentException("Order not found");
        }
        
        Order order = orderOptional.get();
        
        // Update status for pre-pickup payment items
        for (OrderItem item : order.getOrderItems()) {
            if ("pre-pickup".equals(item.getPaymentMethod())) {
                item.setPaymentStatus("PAID");
            }
        }
        
        orderRepository.save(order);
    }
    
    /**
     * Accept an order (farmer action)
     */
    @Transactional
    public void acceptOrder(String orderNumber, Farmer farmer) {
        Order order = getOrderByOrderNumber(orderNumber);
        
        // Security check - ensure order contains products from this farmer
        boolean hasProductsFromFarmer = order.getOrderItems().stream()
                .anyMatch(item -> item.getProduct().getFarmer().getId().equals(farmer.getId()));
        
        if (!hasProductsFromFarmer) {
            throw new IllegalArgumentException("Order does not contain products from this farmer");
        }
        
        // Update order status to READY_FOR_PICKUP
        order.setStatus("READY_FOR_PICKUP");
        orderRepository.save(order);
        
        logger.info("Order {} accepted by farmer {}", orderNumber, farmer.getUniqueId());
    }
    
    /**
     * Finalize an order (consumer action)
     */
    @Transactional
    public void finalizeOrder(String orderNumber, Consumer consumer) {
        Order order = getOrderByOrderNumber(orderNumber);
        
        // Security check - ensure order belongs to this consumer
        if (!order.getConsumer().getId().equals(consumer.getId())) {
            throw new IllegalArgumentException("Order does not belong to this consumer");
        }
        
        // Check if order is ready for pickup
        if (!"READY_FOR_PICKUP".equals(order.getStatus())) {
            throw new IllegalArgumentException("Order is not ready for pickup");
        }
        
        // Update order status to COMPLETED
        order.setStatus("COMPLETED");
        orderRepository.save(order);
        
        logger.info("Order {} finalized by consumer {}", orderNumber, consumer.getUniqueId());
    }
    
    /**
     * Get orders for a consumer
     */
    public List<Order> getOrdersForConsumer(Consumer consumer) {
        return orderRepository.findByConsumer(consumer);
    }
    
    /**
     * Get orders for a farmer
     */
    public List<OrderItem> getOrderItemsForFarmer(Farmer farmer) {
        return orderItemRepository.findByProductFarmer(farmer);
    }
    
    /**
     * Get order by order number
     */
    public Order getOrderByOrderNumber(String orderNumber) {
        Optional<Order> orderOptional = orderRepository.findByOrderNumber(orderNumber);
        
        if (!orderOptional.isPresent()) {
            throw new IllegalArgumentException("Order not found");
        }
        
        return orderOptional.get();
    }
    
    /**
     * Generate a unique order number
     */
    private String generateOrderNumber() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String datePart = LocalDateTime.now().format(formatter);
        
        // Generate random 6-digit number
        Random random = new Random();
        int randomNum = 100000 + random.nextInt(900000);
        
        return "ORD-" + datePart + "-" + randomNum;
    }
    
    /**
     * Get order item by ID
     */
    public OrderItem getOrderItemById(Long orderItemId) {
        Optional<OrderItem> orderItemOptional = orderItemRepository.findById(orderItemId);
        
        if (!orderItemOptional.isPresent()) {
            throw new IllegalArgumentException("Order item not found");
        }
        
        return orderItemOptional.get();
    }
    
    /**
     * Update payment status for an order item
     */
    @Transactional
    public void updateOrderItemPaymentStatus(Long orderItemId, String paymentStatus) {
        OrderItem orderItem = getOrderItemById(orderItemId);
        orderItem.setPaymentStatus(paymentStatus);
        orderItemRepository.save(orderItem);
        
        // Update order status if all items are paid
        Order order = orderItem.getOrder();
        boolean allItemsPaid = order.getOrderItems().stream()
                .allMatch(item -> "PAID".equals(item.getPaymentStatus()));
        
        if (allItemsPaid) {
            order.setStatus("COMPLETED");
            orderRepository.save(order);
        }
        
        logger.info("Updated payment status for order item {} to {}", orderItemId, paymentStatus);
    }
}

package com.example.demo.controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.CartItem;
import com.example.demo.model.Consumer;
import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;
import com.example.demo.model.Product;
import com.example.demo.model.User;
import com.example.demo.service.CartService;
import com.example.demo.service.OrderService;
import com.example.demo.service.ProductService;
import com.example.demo.service.UserService;

@Controller
@RequestMapping("/consumer")
public class ConsumerController {
    
    private static final Logger logger = LoggerFactory.getLogger(ConsumerController.class);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private CartService cartService;
    
    @Autowired
    private OrderService orderService;
    
    @GetMapping("/products")
    public String viewAllProducts(Model model, Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"CONSUMER".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        Consumer consumer = (Consumer) user;
        List<Product> products = productService.getAllProducts();
        
        // Load the type-and-product.json file
        try {
            Resource resource = new ClassPathResource("static/data/type-and-product.json");
            
            if (resource.exists()) {
                Path path = Paths.get(resource.getURI());
                String jsonContent = new String(Files.readAllBytes(path));
                model.addAttribute("typeAndProductJson", jsonContent);
                logger.info("Successfully loaded type-and-product.json from classpath");
            } else {
                model.addAttribute("jsonError", "Product types JSON file not found");
                logger.warn("type-and-product.json not found in classpath");
            }
        } catch (Exception e) {
            model.addAttribute("jsonError", e.getMessage());
            logger.error("Error loading type-and-product.json: {}", e.getMessage(), e);
        }
        
        model.addAttribute("products", products);
        model.addAttribute("user", consumer);
        
        return "consumer-products";
    }
    
    @GetMapping("/product/{productId}")
    public String viewProductDetails(@PathVariable String productId, Model model, Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"CONSUMER".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        Consumer consumer = (Consumer) user;
        Product product = productService.getProductByProductId(productId);
        
        if (product == null) {
            return "redirect:/consumer-dashboard";
        }
        
        // Check if product is admin-deleted
        if (product.getAdminDeleted()) {
            model.addAttribute("message", "This product is no longer available.");
            return "product-unavailable";
        }
        
        model.addAttribute("product", product);
        model.addAttribute("user", consumer);
        
        return "consumer-product-details";
    }
    
    @GetMapping("/my-kart")
    public String viewCart(Model model, Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"CONSUMER".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        Consumer consumer = (Consumer) user;
        List<CartItem> cartItems = cartService.getCartItems(consumer);
        double cartTotal = cartService.calculateCartTotal(cartItems);
        
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartTotal", cartTotal);
        model.addAttribute("user", consumer);
        
        return "consumer-my-kart";
    }
    
    @PostMapping("/add-to-cart")
    public String addToCart(
            @RequestParam("productId") String productId,
            @RequestParam("quantity") Integer quantity,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"CONSUMER".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        Consumer consumer = (Consumer) user;
        
        try {
            cartService.addToCart(consumer, productId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Product added to cart successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            logger.error("Error adding product to cart: {}", e.getMessage());
        }
        
        return "redirect:/consumer/product/" + productId;
    }
    
    @PostMapping("/update-cart-item")
    public String updateCartItem(
            @RequestParam("cartItemId") Long cartItemId,
            @RequestParam("quantity") Integer quantity,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"CONSUMER".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        Consumer consumer = (Consumer) user;
        
        try {
            cartService.updateCartItemQuantity(consumer, cartItemId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Cart updated successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            logger.error("Error updating cart item: {}", e.getMessage());
        }
        
        return "redirect:/consumer/my-kart";
    }
    
    @PostMapping("/remove-from-cart")
    public String removeFromCart(
            @RequestParam("cartItemId") Long cartItemId,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"CONSUMER".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        Consumer consumer = (Consumer) user;
        
        try {
            cartService.removeFromCart(consumer, cartItemId);
            redirectAttributes.addFlashAttribute("successMessage", "Item removed from cart successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            logger.error("Error removing item from cart: {}", e.getMessage());
        }
        
        return "redirect:/consumer/my-kart";
    }
    
    @PostMapping("/checkout")
    public String checkout(Principal principal, RedirectAttributes redirectAttributes) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"CONSUMER".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        Consumer consumer = (Consumer) user;
        List<CartItem> cartItems = cartService.getCartItems(consumer);
        
        if (cartItems.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Your cart is empty");
            return "redirect:/consumer/my-kart";
        }
        
        try {
            // Group cart items by payment method
            Map<String, List<CartItem>> itemsByPaymentMethod = cartItems.stream()
                    .collect(Collectors.groupingBy(item -> item.getProduct().getPaymentOption()));
            
            // Create order
            Order order = orderService.createOrderFromCart(consumer, cartItems);
            
            // If there are pre-pickup payment items, redirect to payment page
            if (itemsByPaymentMethod.containsKey("pre-pickup")) {
                return "redirect:/consumer/payment/" + order.getOrderNumber();
            } else {
                // All items are pay-on-pickup, redirect to orders page
                redirectAttributes.addFlashAttribute("successMessage", "Order placed successfully");
                return "redirect:/consumer/my-orders";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating order: " + e.getMessage());
            logger.error("Error creating order: {}", e.getMessage(), e);
            return "redirect:/consumer/my-kart";
        }
    }
    
    @GetMapping("/payment/{orderNumber}")
    public String showPaymentPage(@PathVariable String orderNumber, Model model, Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"CONSUMER".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        Consumer consumer = (Consumer) user;
        
        try {
            Order order = orderService.getOrderByOrderNumber(orderNumber);
            
            // Security check - ensure order belongs to this consumer
            if (!order.getConsumer().getId().equals(consumer.getId())) {
                return "redirect:/consumer-dashboard";
            }
            
            // Get pre-pickup payment items
            List<OrderItem> prePickupItems = order.getOrderItems().stream()
                    .filter(item -> "pre-pickup".equals(item.getPaymentMethod()))
                    .collect(Collectors.toList());
            
            model.addAttribute("order", order);
            model.addAttribute("prePickupItems", prePickupItems);
            model.addAttribute("user", consumer);
            
            return "consumer-payment";
        } catch (Exception e) {
            logger.error("Error showing payment page: {}", e.getMessage(), e);
            return "redirect:/consumer-dashboard";
        }
    }
    
    @PostMapping("/complete-payment")
    public String completePayment(@RequestParam("orderNumber") String orderNumber, Principal principal, RedirectAttributes redirectAttributes) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"CONSUMER".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        Consumer consumer = (Consumer) user;
        
        try {
            Order order = orderService.getOrderByOrderNumber(orderNumber);
            
            // Security check - ensure order belongs to this consumer
            if (!order.getConsumer().getId().equals(consumer.getId())) {
                return "redirect:/consumer-dashboard";
            }
            
            // Process payment
            orderService.processPayment(orderNumber);
            
            redirectAttributes.addFlashAttribute("successMessage", "Payment completed successfully");
            return "redirect:/consumer/my-orders";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error processing payment: " + e.getMessage());
            logger.error("Error processing payment: {}", e.getMessage(), e);
            return "redirect:/consumer/payment/" + orderNumber;
        }
    }
    
    @GetMapping("/my-orders")
    public String viewOrders(Model model, Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"CONSUMER".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        Consumer consumer = (Consumer) user;
        List<Order> orders = orderService.getOrdersForConsumer(consumer);
        
        model.addAttribute("orders", orders);
        model.addAttribute("user", consumer);
        
        return "consumer-my-orders";
    }
    
    @GetMapping("/order/{orderNumber}")
    public String viewOrderDetails(@PathVariable String orderNumber, Model model, Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"CONSUMER".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        Consumer consumer = (Consumer) user;
        
        try {
            Order order = orderService.getOrderByOrderNumber(orderNumber);
            
            // Security check - ensure order belongs to this consumer
            if (!order.getConsumer().getId().equals(consumer.getId())) {
                return "redirect:/consumer-dashboard";
            }
            
            model.addAttribute("order", order);
            model.addAttribute("user", consumer);
            
            return "consumer-order-details";
        } catch (Exception e) {
            logger.error("Error viewing order details: {}", e.getMessage(), e);
            return "redirect:/consumer/my-orders";
        }
    }
    
    @PostMapping("/finalize-order")
    public String finalizeOrder(
            @RequestParam("orderNumber") String orderNumber,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"CONSUMER".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        Consumer consumer = (Consumer) user;
        
        try {
            // Finalize the order
            orderService.finalizeOrder(orderNumber, consumer);
            
            redirectAttributes.addFlashAttribute("successMessage", "Order finalized successfully. Thank you for your purchase!");
            return "redirect:/consumer/order/" + orderNumber;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error finalizing order: " + e.getMessage());
            logger.error("Error finalizing order: {}", e.getMessage(), e);
            return "redirect:/consumer/my-orders";
        }
    }
}

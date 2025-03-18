package com.example.demo.controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.Consumer;
import com.example.demo.model.Farmer;
import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;
import com.example.demo.model.Product;
import com.example.demo.model.User;
import com.example.demo.service.OrderService;
import com.example.demo.service.ProductService;
import com.example.demo.service.UserService;

@Controller
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private OrderService orderService;

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid credentials. Please try again.");
        }
        return "login";
    }
    
    @GetMapping("/login/adminportal")
    public String adminLogin(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid admin credentials. Please try again.");
        }
        logger.info("Accessing admin login portal");
        return "admin-login";
    }
    
    @GetMapping("/login-success")
    public String loginSuccess() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.info("Login success handler called with authentication: {}", authentication);
        
        if (authentication != null && authentication.isAuthenticated()) {
            if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_FARMER"))) {
                logger.info("Redirecting to farmer dashboard");
                return "redirect:/farmer-dashboard";
            } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CONSUMER"))) {
                logger.info("Redirecting to consumer dashboard");
                return "redirect:/consumer-dashboard";
            } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
                logger.info("Redirecting to admin dashboard");
                return "redirect:/admin-dashboard";
            }
        }
        
        logger.warn("No specific role found or not authenticated, redirecting to home");
        return "redirect:/";
    }

    @GetMapping("/farmer-dashboard")
    public String farmerDashboard(Model model, Principal principal) {
        logger.info("Accessing farmer dashboard for user: {}", principal.getName());
        
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"FARMER".equals(user.getUserType())) {
            logger.warn("User is not a farmer: {}", principal.getName());
            return "redirect:/login";
        }
        
        // Check if the user is admin-deleted
        if (user.getAdminDeleted()) {
            logger.warn("User account has been deleted by admin: {}", principal.getName());
            model.addAttribute("message", "Your account has been deleted by the administrator. Please contact support for assistance.");
            return "account-deleted";
        }
        
        Farmer farmer = (Farmer) user;
        List<Product> farmerProducts;
        
        try {
            farmerProducts = productService.getProductsByFarmerId(farmer.getUniqueId());
            logger.info("Found {} products for farmer {}", farmerProducts.size(), farmer.getUniqueId());
            
            // Get order items for this farmer to calculate total sales
            List<OrderItem> orderItems = orderService.getOrderItemsForFarmer(farmer);
            
            // Calculate total sales amount (only for PAID items)
            double totalSales = orderItems.stream()
                    .filter(item -> "PAID".equals(item.getPaymentStatus()))
                    .mapToDouble(OrderItem::getTotalPrice)
                    .sum();
            
            // Get recent orders (unique orders, sorted by date)
            List<Order> recentOrders = orderItems.stream()
                    .map(OrderItem::getOrder)
                    .distinct()
                    .sorted((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate()))
                    .limit(5)  // Show only 5 most recent orders
                    .collect(Collectors.toList());
            
            model.addAttribute("totalSales", totalSales);
            model.addAttribute("recentOrders", recentOrders);
            
        } catch (Exception e) {
            logger.error("Error fetching data for farmer dashboard: {}", e.getMessage(), e);
            farmerProducts = new ArrayList<>();
            model.addAttribute("totalSales", 0.0);
            model.addAttribute("recentOrders", new ArrayList<>());
        }
        
        model.addAttribute("farmer", farmer);
        model.addAttribute("products", farmerProducts);
        
        return "farmer-dashboard";
    }
    
    @GetMapping("/consumer-dashboard")
    public String consumerDashboard(Model model, Principal principal) {
        logger.info("Accessing consumer dashboard for user: {}", principal.getName());
        
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"CONSUMER".equals(user.getUserType())) {
            logger.warn("User is not a consumer: {}", principal.getName());
            return "redirect:/login";
        }
        
        // Check if the user is admin-deleted
        if (user.getAdminDeleted()) {
            logger.warn("User account has been deleted by admin: {}", principal.getName());
            model.addAttribute("message", "Your account has been deleted by the administrator. Please contact support for assistance.");
            return "account-deleted";
        }
        
        Consumer consumer = (Consumer) user;
        List<Product> allProducts;
        
        try {
            allProducts = productService.getAllProducts();
            logger.info("Found {} products for consumer dashboard", allProducts.size());
        } catch (Exception e) {
            logger.error("Error fetching products for consumer: {}", e.getMessage(), e);
            allProducts = new ArrayList<>();
        }
        
        // Load the type-and-product.json file
        try {
            org.springframework.core.io.Resource resource = 
                new org.springframework.core.io.ClassPathResource("static/data/type-and-product.json");
            
            if (resource.exists()) {
                java.nio.file.Path path = java.nio.file.Paths.get(resource.getURI());
                String jsonContent = new String(java.nio.file.Files.readAllBytes(path));
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
        
        model.addAttribute("user", consumer);
        model.addAttribute("products", allProducts);
        
        return "consumer-dashboard";
    }
    
    @GetMapping("/logout-success")
    public String logoutSuccess() {
        logger.info("User has been logged out successfully");
        return "logout-success";
    }
}

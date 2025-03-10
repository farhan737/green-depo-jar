package com.example.demo.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Farmer;
import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;
import com.example.demo.model.Product;
import com.example.demo.model.User;
import com.example.demo.service.OrderService;
import com.example.demo.service.ProductService;
import com.example.demo.service.UserService;

@Controller
@RequestMapping("/farmer")
public class FarmerController {
    
    private static final Logger logger = LoggerFactory.getLogger(FarmerController.class);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private ProductService productService;
    
    @GetMapping("/orders")
    public String viewOrders(Model model, Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"FARMER".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        Farmer farmer = (Farmer) user;
        List<OrderItem> orderItems = orderService.getOrderItemsForFarmer(farmer);
        
        // Group order items by order number
        Map<String, List<OrderItem>> orderItemsByOrder = orderItems.stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getOrderNumber()));
        
        model.addAttribute("orderItemsByOrder", orderItemsByOrder);
        model.addAttribute("user", farmer);
        
        return "farmer-orders";
    }
    
    @GetMapping("/order/{orderNumber}")
    public String viewOrderDetails(@PathVariable String orderNumber, Model model, Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"FARMER".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        Farmer farmer = (Farmer) user;
        
        try {
            logger.info("Farmer {} accessing order details for order {}", farmer.getUniqueId(), orderNumber);
            
            // Get the order first
            Order order = orderService.getOrderByOrderNumber(orderNumber);
            if (order == null) {
                logger.warn("Order {} not found", orderNumber);
                model.addAttribute("errorMessage", "Order not found");
                model.addAttribute("user", farmer);
                return "farmer-orders";
            }
            
            // Get order items for this farmer and order
            List<OrderItem> orderItems = orderService.getOrderItemsForFarmer(farmer).stream()
                    .filter(item -> item.getOrder().getOrderNumber().equals(orderNumber))
                    .collect(Collectors.toList());
            
            if (orderItems.isEmpty()) {
                logger.warn("No order items found for order {} and farmer {}", orderNumber, farmer.getUniqueId());
                model.addAttribute("errorMessage", "No items found in this order for your products");
                model.addAttribute("user", farmer);
                return "farmer-orders";
            }
            
            // Calculate total for farmer's products in this order
            double farmerProductsTotal = orderItems.stream()
                    .mapToDouble(OrderItem::getTotalPrice)
                    .sum();
            
            model.addAttribute("orderItems", orderItems);
            model.addAttribute("order", order);
            model.addAttribute("user", farmer);
            model.addAttribute("farmerProductsTotal", farmerProductsTotal);
            
            return "farmer-order-details";
        } catch (Exception e) {
            logger.error("Error viewing order details for order {}: {}", orderNumber, e.getMessage(), e);
            model.addAttribute("errorMessage", "An error occurred while loading the order details. Please try again later.");
            model.addAttribute("user", farmer);
            return "farmer-orders";
        }
    }
    
    @PostMapping("/accept-order")
    public String acceptOrder(
            @RequestParam("orderNumber") String orderNumber,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"FARMER".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        Farmer farmer = (Farmer) user;
        
        try {
            // Accept the order
            orderService.acceptOrder(orderNumber, farmer);
            
            redirectAttributes.addFlashAttribute("successMessage", "Order accepted successfully. It is now ready for pickup.");
            return "redirect:/farmer/order/" + orderNumber;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error accepting order: " + e.getMessage());
            logger.error("Error accepting order: {}", e.getMessage(), e);
            return "redirect:/farmer/orders";
        }
    }
    
    @PostMapping("/update-payment-status")
    public String updatePaymentStatus(
            @RequestParam("orderItemId") Long orderItemId,
            @RequestParam("paymentStatus") String paymentStatus,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"FARMER".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        Farmer farmer = (Farmer) user;
        
        try {
            // Get order item
            OrderItem orderItem = orderService.getOrderItemById(orderItemId);
            
            // Security check - ensure order item belongs to this farmer
            if (!orderItem.getProduct().getFarmer().getId().equals(farmer.getId())) {
                return "redirect:/farmer/orders";
            }
            
            // Update payment status
            orderService.updateOrderItemPaymentStatus(orderItemId, paymentStatus);
            
            redirectAttributes.addFlashAttribute("successMessage", "Payment status updated successfully");
            return "redirect:/farmer/order/" + orderItem.getOrder().getOrderNumber();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating payment status: " + e.getMessage());
            logger.error("Error updating payment status: {}", e.getMessage(), e);
            return "redirect:/farmer/orders";
        }
    }
    
    @GetMapping("/sales-analytics")
    public String viewSalesAnalytics(Model model, Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"FARMER".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        Farmer farmer = (Farmer) user;
        
        try {
            // Get all products for this farmer
            List<Product> products = productService.getProductsByFarmer(farmer);
            
            // Get all order items for this farmer
            List<OrderItem> orderItems = orderService.getOrderItemsForFarmer(farmer);
            
            // Calculate total sales amount
            double totalSales = orderItems.stream()
                    .filter(item -> "PAID".equals(item.getPaymentStatus()))
                    .mapToDouble(OrderItem::getTotalPrice)
                    .sum();
            
            // Calculate sales per product
            Map<Product, Double> salesPerProduct = orderItems.stream()
                    .filter(item -> "PAID".equals(item.getPaymentStatus()))
                    .collect(Collectors.groupingBy(
                            OrderItem::getProduct,
                            Collectors.summingDouble(OrderItem::getTotalPrice)
                    ));
            
            // Create a map with product IDs as keys for JavaScript
            Map<String, Double> salesPerProductId = new HashMap<>();
            salesPerProduct.forEach((product, sales) -> {
                salesPerProductId.put(product.getProductId(), sales);
            });
            
            // Calculate quantity sold per product
            Map<Product, Integer> quantitySoldPerProduct = orderItems.stream()
                    .filter(item -> "PAID".equals(item.getPaymentStatus()))
                    .collect(Collectors.groupingBy(
                            OrderItem::getProduct,
                            Collectors.summingInt(OrderItem::getQuantity)
                    ));
            
            model.addAttribute("products", products);
            model.addAttribute("totalSales", totalSales);
            model.addAttribute("salesPerProduct", salesPerProduct);
            model.addAttribute("salesPerProductId", salesPerProductId);
            model.addAttribute("quantitySoldPerProduct", quantitySoldPerProduct);
            model.addAttribute("user", farmer);
            
            return "farmer-sales-analytics";
        } catch (Exception e) {
            logger.error("Error viewing sales analytics: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "An error occurred while loading sales analytics. Please try again later.");
            model.addAttribute("user", farmer);
            return "farmer-dashboard";
        }
    }
    
    @GetMapping("/product-analytics/{productId}")
    public String viewProductAnalytics(@PathVariable String productId, Model model, Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"FARMER".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        Farmer farmer = (Farmer) user;
        
        try {
            // Get the product
            Product product = productService.getProductByProductId(productId);
            
            // Security check - ensure product belongs to this farmer
            if (product == null || !product.getFarmer().getId().equals(farmer.getId())) {
                return "redirect:/farmer/sales-analytics";
            }
            
            // Get all order items for this product
            List<OrderItem> productOrderItems = orderService.getOrderItemsForFarmer(farmer).stream()
                    .filter(item -> item.getProduct().getProductId().equals(productId))
                    .collect(Collectors.toList());
            
            // Calculate total sales for this product
            double totalProductSales = productOrderItems.stream()
                    .filter(item -> "PAID".equals(item.getPaymentStatus()))
                    .mapToDouble(OrderItem::getTotalPrice)
                    .sum();
            
            // Calculate total quantity sold
            int totalQuantitySold = productOrderItems.stream()
                    .filter(item -> "PAID".equals(item.getPaymentStatus()))
                    .mapToInt(OrderItem::getQuantity)
                    .sum();
            
            // Group orders by date to show sales trend
            Map<LocalDateTime, Double> salesByDate = productOrderItems.stream()
                    .filter(item -> "PAID".equals(item.getPaymentStatus()))
                    .collect(Collectors.groupingBy(
                            item -> item.getOrder().getOrderDate(),
                            Collectors.summingDouble(OrderItem::getTotalPrice)
                    ));
            
            model.addAttribute("product", product);
            model.addAttribute("totalProductSales", totalProductSales);
            model.addAttribute("totalQuantitySold", totalQuantitySold);
            model.addAttribute("salesByDate", salesByDate);
            model.addAttribute("orderItems", productOrderItems);
            model.addAttribute("user", farmer);
            
            return "farmer-product-analytics";
        } catch (Exception e) {
            logger.error("Error viewing product analytics: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "An error occurred while loading product analytics. Please try again later.");
            model.addAttribute("user", farmer);
            return "farmer-sales-analytics";
        }
    }
}

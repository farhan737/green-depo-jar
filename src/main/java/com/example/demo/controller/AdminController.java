package com.example.demo.controller;

import java.security.Principal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Consumer;
import com.example.demo.model.Farmer;
import com.example.demo.model.Order;
import com.example.demo.model.PriceLimit;
import com.example.demo.model.Product;
import com.example.demo.model.User;
import com.example.demo.service.OrderService;
import com.example.demo.service.PriceLimitService;
import com.example.demo.service.ProductService;
import com.example.demo.service.UserService;

@Controller
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private UserService userService;
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private PriceLimitService priceLimitService;
    
    @GetMapping("/admin-dashboard")
    public String adminDashboard(Model model, Principal principal) {
        logger.info("Accessing admin dashboard for user: {}", principal.getName());
        
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"ADMIN".equals(user.getUserType())) {
            logger.warn("User is not an admin: {}", principal.getName());
            return "redirect:/login";
        }
        
        List<Farmer> farmers = userService.getAllFarmers();
        List<Consumer> consumers = userService.getAllConsumers();
        List<Product> products = productService.getAllProducts();
        
        // Count deleted items
        long deletedFarmers = farmers.stream().filter(farmer -> farmer.getAdminDeleted()).count();
        long deletedConsumers = consumers.stream().filter(consumer -> consumer.getAdminDeleted()).count();
        long deletedProducts = products.stream().filter(product -> product.getAdminDeleted()).count();
        
        model.addAttribute("user", user);
        model.addAttribute("farmers", farmers);
        model.addAttribute("consumers", consumers);
        model.addAttribute("products", products);
        model.addAttribute("deletedFarmers", deletedFarmers);
        model.addAttribute("deletedConsumers", deletedConsumers);
        model.addAttribute("deletedProducts", deletedProducts);
        
        return "admin-dashboard";
    }
    
    @GetMapping("/admin/farmers")
    public String viewFarmers(Model model, Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"ADMIN".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        List<Farmer> farmers = userService.getAllFarmers();
        model.addAttribute("user", user);
        model.addAttribute("farmers", farmers);
        
        return "admin-farmers";
    }
    
    @GetMapping("/admin/consumers")
    public String viewConsumers(Model model, Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"ADMIN".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        List<Consumer> consumers = userService.getAllConsumers();
        model.addAttribute("user", user);
        model.addAttribute("consumers", consumers);
        
        return "admin-consumers";
    }
    
    @GetMapping("/admin/products")
    public String viewProducts(Model model, Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"ADMIN".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        List<Product> products = productService.getAllProducts();
        model.addAttribute("user", user);
        model.addAttribute("products", products);
        
        return "admin-products";
    }
    
    @GetMapping("/admin/farmer/{uniqueId}")
    public String viewFarmerDetails(@PathVariable String uniqueId, Model model, Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"ADMIN".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        Farmer farmer = userService.getFarmerByUniqueId(uniqueId);
        if (farmer == null) {
            return "redirect:/admin/farmers";
        }
        
        // Get products for this farmer
        List<Product> farmerProducts = productService.getProductsByFarmer(farmer);
        
        model.addAttribute("user", user);
        model.addAttribute("farmer", farmer);
        model.addAttribute("farmerProducts", farmerProducts);
        
        return "admin-farmer-details";
    }
    
    @GetMapping("/admin/consumer/{uniqueId}")
    public String viewConsumerDetails(@PathVariable String uniqueId, Model model, Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"ADMIN".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        Consumer consumer = userService.getConsumerByUniqueId(uniqueId);
        if (consumer == null) {
            return "redirect:/admin/consumers";
        }
        
        // Get orders for this consumer
        List<Order> consumerOrders = orderService.getOrdersForConsumer(consumer);
        
        model.addAttribute("user", user);
        model.addAttribute("consumer", consumer);
        model.addAttribute("consumerOrders", consumerOrders);
        
        return "admin-consumer-details";
    }
    
    @GetMapping("/admin/product/{productId}")
    public String viewProductDetails(@PathVariable String productId, Model model, Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"ADMIN".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        Product product = productService.getProductById(productId);
        if (product == null) {
            return "redirect:/admin/products";
        }
        
        model.addAttribute("user", user);
        model.addAttribute("product", product);
        
        return "admin-product-details";
    }
    
    @GetMapping("/admin/order/{orderNumber}")
    public String viewOrderDetails(@PathVariable String orderNumber, Model model, Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"ADMIN".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        try {
            Order order = orderService.getOrderByOrderNumber(orderNumber);
            model.addAttribute("user", user);
            model.addAttribute("order", order);
            return "admin-order-details";
        } catch (IllegalArgumentException e) {
            return "redirect:/admin-dashboard";
        }
    }
    
    @GetMapping("/admin/market-prices")
    public String viewMarketPrices(Model model, Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"ADMIN".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        List<PriceLimit> priceLimits = priceLimitService.getAllPriceLimits();
        model.addAttribute("user", user);
        model.addAttribute("priceLimits", priceLimits);
        
        return "admin-market-prices";
    }
    
    @PostMapping("/admin/delete-farmer")
    public String deleteFarmer(@RequestParam String uniqueId, RedirectAttributes redirectAttributes, Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"ADMIN".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        try {
            userService.deleteFarmer(uniqueId);
            redirectAttributes.addFlashAttribute("successMessage", "Farmer deleted successfully");
        } catch (Exception e) {
            logger.error("Error deleting farmer: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting farmer: " + e.getMessage());
        }
        
        return "redirect:/admin/farmers";
    }
    
    @PostMapping("/admin/delete-consumer")
    public String deleteConsumer(@RequestParam String uniqueId, RedirectAttributes redirectAttributes, Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"ADMIN".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        try {
            userService.deleteConsumer(uniqueId);
            redirectAttributes.addFlashAttribute("successMessage", "Consumer deleted successfully");
        } catch (Exception e) {
            logger.error("Error deleting consumer: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting consumer: " + e.getMessage());
        }
        
        return "redirect:/admin/consumers";
    }
    
    @PostMapping("/admin/delete-product")
    public String deleteProduct(@RequestParam String productId, RedirectAttributes redirectAttributes, Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"ADMIN".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        try {
            productService.deleteProduct(productId);
            redirectAttributes.addFlashAttribute("successMessage", "Product deleted successfully");
        } catch (IllegalStateException e) {
            // Product is referenced in orders, mark it as unavailable instead
            logger.info("Product {} cannot be deleted, marking as unavailable instead", productId);
            try {
                productService.markProductAsUnavailable(productId);
                redirectAttributes.addFlashAttribute("warningMessage", 
                    "Product could not be deleted because it is referenced in orders. It has been marked as unavailable instead.");
            } catch (Exception ex) {
                logger.error("Error marking product as unavailable: {}", ex.getMessage(), ex);
                redirectAttributes.addFlashAttribute("errorMessage", "Error processing request: " + ex.getMessage());
            }
        } catch (Exception e) {
            logger.error("Error deleting product: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting product: " + e.getMessage());
        }
        
        return "redirect:/admin/products";
    }
    
    @PostMapping("/admin/market-prices/save")
    public String saveMarketPrice(
            @RequestParam String productType,
            @RequestParam String productName,
            @RequestParam Double pricePerGram,
            @RequestParam Double pricePerKg,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"ADMIN".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        try {
            PriceLimit priceLimit = new PriceLimit(productType, productName, pricePerGram, pricePerKg);
            priceLimitService.savePriceLimit(priceLimit);
            redirectAttributes.addFlashAttribute("successMessage", "Price limit saved successfully");
        } catch (Exception e) {
            logger.error("Error saving price limit: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving price limit: " + e.getMessage());
        }
        
        return "redirect:/admin/market-prices";
    }
    
    @PostMapping("/admin/market-prices/update")
    public String updateMarketPrice(
            @RequestParam Long id,
            @RequestParam String productType,
            @RequestParam String productName,
            @RequestParam Double pricePerGram,
            @RequestParam Double pricePerKg,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"ADMIN".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        try {
            PriceLimit priceLimit = new PriceLimit(productType, productName, pricePerGram, pricePerKg);
            priceLimit.setId(id);
            priceLimitService.savePriceLimit(priceLimit);
            redirectAttributes.addFlashAttribute("successMessage", "Price limit updated successfully");
        } catch (Exception e) {
            logger.error("Error updating price limit: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating price limit: " + e.getMessage());
        }
        
        return "redirect:/admin/market-prices";
    }
    
    @PostMapping("/admin/market-prices/delete")
    public String deleteMarketPrice(
            @RequestParam Long id,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"ADMIN".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        try {
            priceLimitService.deletePriceLimit(id);
            redirectAttributes.addFlashAttribute("successMessage", "Price limit deleted successfully");
        } catch (Exception e) {
            logger.error("Error deleting price limit: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting price limit: " + e.getMessage());
        }
        
        return "redirect:/admin/market-prices";
    }
    
    @PostMapping("/admin/restore-farmer/{uniqueId}")
    public String restoreFarmer(@PathVariable String uniqueId, RedirectAttributes redirectAttributes) {
        try {
            userService.restoreFarmer(uniqueId);
            redirectAttributes.addFlashAttribute("successMessage", "Farmer has been successfully restored.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error restoring farmer: " + e.getMessage());
            logger.error("Error restoring farmer with uniqueId: {}", uniqueId, e);
        }
        return "redirect:/admin/farmers";
    }
    
    @PostMapping("/admin/restore-consumer/{uniqueId}")
    public String restoreConsumer(@PathVariable String uniqueId, RedirectAttributes redirectAttributes) {
        try {
            userService.restoreConsumer(uniqueId);
            redirectAttributes.addFlashAttribute("successMessage", "Consumer has been successfully restored.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error restoring consumer: " + e.getMessage());
            logger.error("Error restoring consumer with uniqueId: {}", uniqueId, e);
        }
        return "redirect:/admin/consumers";
    }
    
    @PostMapping("/admin/restore-product/{productId}")
    public String restoreProduct(@PathVariable String productId, RedirectAttributes redirectAttributes) {
        try {
            productService.restoreProduct(productId);
            redirectAttributes.addFlashAttribute("successMessage", "Product has been successfully restored.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error restoring product: " + e.getMessage());
            logger.error("Error restoring product with productId: {}", productId, e);
        }
        return "redirect:/admin/products";
    }
    
    @PostMapping("/admin/update-farmer")
    public String updateFarmer(@RequestParam String uniqueId, 
                              @RequestParam String firstName,
                              @RequestParam String lastName,
                              @RequestParam String phoneNumber,
                              @RequestParam String email,
                              RedirectAttributes redirectAttributes,
                              Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"ADMIN".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        try {
            userService.updateFarmerDetails(uniqueId, firstName, lastName, phoneNumber, email);
            redirectAttributes.addFlashAttribute("successMessage", "Farmer updated successfully");
        } catch (Exception e) {
            logger.error("Error updating farmer: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating farmer: " + e.getMessage());
        }
        
        return "redirect:/admin/farmer/" + uniqueId;
    }
    
    @PostMapping("/admin/update-consumer")
    public String updateConsumer(@RequestParam String uniqueId, 
                                @RequestParam String firstName,
                                @RequestParam String lastName,
                                @RequestParam String phoneNumber,
                                @RequestParam String email,
                                RedirectAttributes redirectAttributes,
                                Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"ADMIN".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        try {
            userService.updateConsumerDetails(uniqueId, firstName, lastName, phoneNumber, email);
            redirectAttributes.addFlashAttribute("successMessage", "Consumer updated successfully");
        } catch (Exception e) {
            logger.error("Error updating consumer: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating consumer: " + e.getMessage());
        }
        
        return "redirect:/admin/consumer/" + uniqueId;
    }
    
    @PostMapping("/admin/update-product")
    public String updateProduct(@RequestParam String productId,
                               @RequestParam String title,
                               @RequestParam String description,
                               @RequestParam double price,
                               @RequestParam double weight,
                               RedirectAttributes redirectAttributes,
                               Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"ADMIN".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        try {
            productService.updateProductDetails(productId, title, description, price, weight);
            redirectAttributes.addFlashAttribute("successMessage", "Product updated successfully");
        } catch (Exception e) {
            logger.error("Error updating product: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating product: " + e.getMessage());
        }
        
        return "redirect:/admin/product/" + productId;
    }
}

package com.example.demo.controller;

import java.io.IOException;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Farmer;
import com.example.demo.model.Product;
import com.example.demo.model.ProductImage;
import com.example.demo.model.User;
import com.example.demo.service.ProductService;
import com.example.demo.service.UserService;

@Controller
@RequestMapping("/farmer")
public class ProductController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/products")
    public String viewProducts(Model model, Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"FARMER".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        Farmer farmer = (Farmer) user;
        List<Product> products = productService.getProductsByFarmer(farmer);
        
        model.addAttribute("products", products);
        return "farmer-products";
    }
    
    @GetMapping("/products/create")
    public String createProductForm(Model model, Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"FARMER".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        Farmer farmer = (Farmer) user;
        model.addAttribute("farmer", farmer);
        
        return "create-product";
    }
    
    @PostMapping("/products/create")
    public String createProduct(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String weightUnit,
            @RequestParam Double weight,
            @RequestParam Double price,
            @RequestParam Integer availableStock,
            @RequestParam Integer purchaseLimit,
            @RequestParam String pickupLocation,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String city,
            @RequestParam String locationLink,
            @RequestParam String paymentOption,
            @RequestParam String productType,
            @RequestParam String productName,
            @RequestParam("images") List<MultipartFile> imageFiles,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"FARMER".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        boolean hasValidImage = imageFiles.stream().anyMatch(file -> !file.isEmpty());
        if (!hasValidImage) {
            redirectAttributes.addFlashAttribute("error", "At least one product image is required");
            return "redirect:/farmer/products/create";
        }
        
        long validImageCount = imageFiles.stream().filter(file -> !file.isEmpty()).count();
        if (validImageCount > 5) {
            redirectAttributes.addFlashAttribute("error", "Maximum 5 product images are allowed");
            return "redirect:/farmer/products/create";
        }
        
        Farmer farmer = (Farmer) user;
        
        if ("profile".equals(pickupLocation)) {
            state = farmer.getState();
            district = farmer.getDistrict();
            city = farmer.getCity();
            locationLink = farmer.getLocationLink();
        }
        
        Product product = new Product();
        product.setTitle(title);
        product.setDescription(description);
        product.setWeightUnit(weightUnit);
        product.setWeight(weight);
        product.setPrice(price);
        product.setAvailableStock(availableStock);
        product.setPurchaseLimit(purchaseLimit);
        product.setPickupLocation(pickupLocation);
        product.setState(state);
        product.setDistrict(district);
        product.setCity(city);
        product.setLocationLink(locationLink);
        product.setPaymentOption(paymentOption);
        product.setProductType(productType);
        product.setProductName(productName);
        
        try {
            Product savedProduct = productService.createProduct(product, farmer, imageFiles);
            redirectAttributes.addFlashAttribute("success", "Product created successfully with ID: " + savedProduct.getProductId());
        } catch (IOException e) {
            logger.error("Error processing product images", e);
            redirectAttributes.addFlashAttribute("error", "Error processing product images: " + e.getMessage());
            return "redirect:/farmer/products/create";
        }
        
        return "redirect:/farmer/products";
    }
    
    @GetMapping("/products/edit/{productId}")
    public String editProductForm(@PathVariable String productId, Model model, Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"FARMER".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        Farmer farmer = (Farmer) user;
        Product product = productService.getProductByProductId(productId);
        
        if (product == null || !product.getFarmer().getId().equals(farmer.getId())) {
            return "redirect:/farmer/products";
        }
        
        model.addAttribute("product", product);
        model.addAttribute("farmer", farmer);
        
        return "edit-product";
    }
    
    @PostMapping("/products/edit/{productId}")
    public String updateProduct(
            @PathVariable String productId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String weightUnit,
            @RequestParam Double weight,
            @RequestParam Double price,
            @RequestParam Integer availableStock,
            @RequestParam Integer purchaseLimit,
            @RequestParam String pickupLocation,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String city,
            @RequestParam String locationLink,
            @RequestParam String paymentOption,
            @RequestParam String productType,
            @RequestParam String productName,
            @RequestParam("images") List<MultipartFile> imageFiles,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"FARMER".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        Farmer farmer = (Farmer) user;
        Product product = productService.getProductByProductId(productId);
        
        if (product == null || !product.getFarmer().getId().equals(farmer.getId())) {
            return "redirect:/farmer/products";
        }
        
        if ("profile".equals(pickupLocation)) {
            state = farmer.getState();
            district = farmer.getDistrict();
            city = farmer.getCity();
            locationLink = farmer.getLocationLink();
        }
        
        product.setTitle(title);
        product.setDescription(description);
        product.setWeightUnit(weightUnit);
        product.setWeight(weight);
        product.setPrice(price);
        product.setAvailableStock(availableStock);
        product.setPurchaseLimit(purchaseLimit);
        product.setPickupLocation(pickupLocation);
        product.setState(state);
        product.setDistrict(district);
        product.setCity(city);
        product.setLocationLink(locationLink);
        product.setPaymentOption(paymentOption);
        product.setProductType(productType);
        product.setProductName(productName);
        
        try {
            productService.updateProduct(product, imageFiles);
            redirectAttributes.addFlashAttribute("success", "Product updated successfully");
        } catch (IOException e) {
            logger.error("Error processing product images", e);
            redirectAttributes.addFlashAttribute("error", "Error processing product images: " + e.getMessage());
            return "redirect:/farmer/products/edit/" + productId;
        }
        
        return "redirect:/farmer/products";
    }
    
    @PostMapping("/products/delete/{productId}")
    public String deleteProduct(@PathVariable String productId, Principal principal, RedirectAttributes redirectAttributes) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null || !"FARMER".equals(user.getUserType())) {
            return "redirect:/login";
        }
        
        Farmer farmer = (Farmer) user;
        Product product = productService.getProductByProductId(productId);
        
        if (product == null || !product.getFarmer().getId().equals(farmer.getId())) {
            return "redirect:/farmer/products";
        }
        
        productService.deleteProduct(product);
        redirectAttributes.addFlashAttribute("success", "Product deleted successfully");
        
        return "redirect:/farmer/products";
    }
    
    @GetMapping("/products/image/{productId}/{imageId}")
    public String viewProductImage(@PathVariable String productId, @PathVariable Long imageId, Model model, Principal principal) {
        User user = userService.findUserByLoginIdentifier(principal.getName());
        if (user == null) {
            return "redirect:/login";
        }
        
        Product product = productService.getProductByProductId(productId);
        if (product == null) {
            return "redirect:/farmer/products";
        }
        
        ProductImage image = product.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElse(null);
        
        if (image == null) {
            return "redirect:/farmer/products";
        }
        
        // Find the index of the image in the product's images list
        int imageIndex = product.getImages().indexOf(image);
        
        model.addAttribute("product", product);
        model.addAttribute("image", image);
        model.addAttribute("imageIndex", imageIndex);
        
        return "product-image";
    }
}

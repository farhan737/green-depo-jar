package com.example.demo.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.Consumer;
import com.example.demo.model.Farmer;
import com.example.demo.model.Order;
import com.example.demo.model.Product;
import com.example.demo.model.ProductImageUrl;
import com.example.demo.model.User;
import com.example.demo.repository.ConsumerRepository;
import com.example.demo.repository.FarmerRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductImageUrlRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.PriceLimitService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/mobile")
public class MobileApiController {

    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private FarmerRepository farmerRepository;
    
    @Autowired
    private ConsumerRepository consumerRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProductImageUrlRepository productImageUrlRepository;
    
    @Autowired
    private PriceLimitService priceLimitService;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserService userService;
    
    private static final Logger logger = LoggerFactory.getLogger(MobileApiController.class);
    
    /**
     * Login endpoint for mobile users
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                Map<String, Object> userData = new HashMap<>();
                userData.put("id", user.getId());
                userData.put("uniqueId", user.getUniqueId());
                userData.put("username", user.getUsername());
                userData.put("firstName", user.getFirstName());
                userData.put("middleName", user.getMiddleName());
                userData.put("lastName", user.getLastName());
                userData.put("email", user.getEmail());
                userData.put("phoneNumber", user.getPhoneNumber());
                userData.put("userType", user.getUserType());
                userData.put("state", user.getState());
                userData.put("district", user.getDistrict());
                userData.put("city", user.getCity());
                userData.put("locationLink", user.getLocationLink() != null ? user.getLocationLink() : "");
                userData.put("dateOfBirth", user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : "");
                
                // Add user type specific details
                if (user.getUserType().equals("FARMER")) {
                    Optional<Farmer> farmerOpt = farmerRepository.findById(user.getId());
                    if (farmerOpt.isPresent()) {
                        Farmer farmer = farmerOpt.get();
                        userData.put("uidNumber", farmer.getUidNumber());
                    }
                } else if (user.getUserType().equals("CONSUMER")) {
                    // We don't need to use the consumer object right now, but we might add consumer-specific fields later
                    // Just check if the consumer exists
                    consumerRepository.findById(user.getId());
                }
                
                // Create a response with token and user data
                Map<String, Object> response = new HashMap<>();
                response.put("token", "Bearer-" + user.getId() + "-" + System.currentTimeMillis()); // Simple token for demo
                response.put("user", userData);
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "User not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid username or password"));
        }
    }
    
    /**
     * Register a new user (consumer or farmer)
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> registerRequest) {
        try {
            // Extract user data from request
            String userType = registerRequest.get("userType");
            String username = registerRequest.get("username");
            String password = registerRequest.get("password");
            String confirmPassword = registerRequest.get("confirmPassword");
            String firstName = registerRequest.get("firstName");
            String middleName = registerRequest.get("middleName");
            String lastName = registerRequest.get("lastName");
            String phoneNumber = registerRequest.get("phoneNumber");
            String email = registerRequest.get("email");
            String dateOfBirthStr = registerRequest.get("dateOfBirth");
            String state = registerRequest.get("state");
            String district = registerRequest.get("district");
            String city = registerRequest.get("city");
            String locationLink = registerRequest.get("locationLink");
            
            // Validate required fields
            if (username == null || password == null || confirmPassword == null || firstName == null || lastName == null || 
                phoneNumber == null || email == null || dateOfBirthStr == null || 
                state == null || district == null || city == null || userType == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false, 
                    "message", "Missing required fields"
                ));
            }
            
            // Check if passwords match
            if (!password.equals(confirmPassword)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false, 
                    "message", "Passwords do not match"
                ));
            }
            
            // Validate password strength
            if (password.length() < 8) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false, 
                    "message", "Password must be at least 8 characters long"
                ));
            }
            
            // Check if username already exists
            if (userService.isUsernameTaken(username)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "success", false, 
                    "message", "Username already exists"
                ));
            }
            
            // Check if email already exists
            if (userService.isEmailTaken(email)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "success", false, 
                    "message", "Email already exists"
                ));
            }
            
            // Check if phone number already exists
            if (userService.isPhoneNumberTaken(phoneNumber)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "success", false, 
                    "message", "Phone number already exists"
                ));
            }
            
            // Parse date of birth
            LocalDate dateOfBirth;
            try {
                dateOfBirth = LocalDate.parse(dateOfBirthStr, DateTimeFormatter.ISO_DATE);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false, 
                    "message", "Invalid date format. Use ISO format (YYYY-MM-DD)"
                ));
            }
            
            // Create user based on type
            User user;
            if ("FARMER".equalsIgnoreCase(userType)) {
                String uidNumber = registerRequest.get("uidNumber");
                if (uidNumber == null) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false, 
                        "message", "UID number is required for farmers"
                    ));
                }
                
                // Check if UID number already exists
                if (userService.isUidNumberTaken(uidNumber)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                        "success", false, 
                        "message", "UID number already exists"
                    ));
                }
                
                // Create farmer - pass null for uniqueId as it will be generated by UserService
                Farmer farmer = new Farmer(null, username, password, firstName, middleName, lastName, 
                                          phoneNumber, email, dateOfBirth, state, district, city, 
                                          locationLink != null ? locationLink : "", uidNumber);
                
                // Register farmer using UserService
                user = userService.registerFarmer(farmer);
            } else if ("CONSUMER".equalsIgnoreCase(userType)) {
                // Create consumer - pass null for uniqueId as it will be generated by UserService
                Consumer consumer = new Consumer(null, username, password, firstName, middleName, lastName, 
                                               phoneNumber, email, dateOfBirth, state, district, city, 
                                               locationLink != null ? locationLink : "");
                
                // Register consumer using UserService
                user = userService.registerConsumer(consumer);
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false, 
                    "message", "Invalid user type. Must be FARMER or CONSUMER"
                ));
            }
            
            // Return success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Registration successful");
            response.put("uniqueId", user.getUniqueId());
            response.put("username", user.getUsername());
            response.put("userType", user.getUserType());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false, 
                "message", "Registration failed: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get all active products
     */
    @GetMapping("/products")
    public ResponseEntity<?> getAllProducts(
            @RequestParam(required = false) String productType,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String district) {
        
        // Get all active products that are not deleted by admin
        List<Product> products = productRepository.findAll().stream()
                .filter(p -> p.getActive() && !p.getAdminDeleted())
                .collect(Collectors.toList());
        
        // Apply filters if provided
        if (productType != null && !productType.isEmpty()) {
            products = products.stream()
                    .filter(p -> productType.equals(p.getProductType()))
                    .collect(Collectors.toList());
        }
        
        if (productName != null && !productName.isEmpty()) {
            products = products.stream()
                    .filter(p -> productName.equals(p.getProductName()))
                    .collect(Collectors.toList());
        }
        
        if (state != null && !state.isEmpty()) {
            products = products.stream()
                    .filter(p -> state.equals(p.getState()))
                    .collect(Collectors.toList());
        }
        
        if (district != null && !district.isEmpty()) {
            products = products.stream()
                    .filter(p -> district.equals(p.getDistrict()))
                    .collect(Collectors.toList());
        }
        
        // Format the response to include properly structured image arrays
        List<Map<String, Object>> formattedProducts = new ArrayList<>();
        
        for (Product product : products) {
            Map<String, Object> formattedProduct = new HashMap<>();
            // Copy all fields from product
            formattedProduct.put("id", product.getId());
            formattedProduct.put("productId", product.getProductId());
            formattedProduct.put("title", product.getTitle());
            formattedProduct.put("description", product.getDescription());
            formattedProduct.put("weightUnit", product.getWeightUnit());
            formattedProduct.put("weight", product.getWeight());
            formattedProduct.put("price", product.getPrice());
            formattedProduct.put("pickupLocation", product.getPickupLocation());
            formattedProduct.put("state", product.getState());
            formattedProduct.put("district", product.getDistrict());
            formattedProduct.put("city", product.getCity());
            formattedProduct.put("locationLink", product.getLocationLink());
            formattedProduct.put("paymentOption", product.getPaymentOption());
            formattedProduct.put("paymentMethod", product.getPaymentMethod());
            formattedProduct.put("availableStock", product.getAvailableStock());
            formattedProduct.put("purchaseLimit", product.getPurchaseLimit());
            formattedProduct.put("active", product.getActive());
            formattedProduct.put("productType", product.getProductType());
            formattedProduct.put("productName", product.getProductName());
            formattedProduct.put("farmer", product.getFarmer());
            
            // Create images array from imageUrls collection
            List<Map<String, Object>> images = new ArrayList<>();
            
            // First check if there are any ProductImageUrl entries
            if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
                for (ProductImageUrl imageUrl : product.getImageUrls()) {
                    Map<String, Object> image = new HashMap<>();
                    image.put("id", imageUrl.getId());
                    image.put("imageUrl", imageUrl.getImageUrl());
                    image.put("displayOrder", imageUrl.getDisplayOrder());
                    images.add(image);
                }
            } 
            // If no ProductImageUrl entries, but there's a single imageUrl, add it to the images array
            else if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                Map<String, Object> image = new HashMap<>();
                image.put("id", 1); // Use a dummy ID
                image.put("imageUrl", product.getImageUrl());
                image.put("displayOrder", 1);
                images.add(image);
            }
            
            formattedProduct.put("images", images);
            formattedProducts.add(formattedProduct);
        }
        
        return ResponseEntity.ok(formattedProducts);
    }
    
    /**
     * Get product by ID
     */
    @GetMapping("/products/{productId}")
    public ResponseEntity<?> getProductById(@PathVariable String productId) {
        Optional<Product> productOpt = productRepository.findByProductId(productId);
        
        if (productOpt.isPresent() && productOpt.get().getActive() && !productOpt.get().getAdminDeleted()) {
            Product product = productOpt.get();
            
            // Format the response to include properly structured image arrays
            Map<String, Object> formattedProduct = new HashMap<>();
            // Copy all fields from product
            formattedProduct.put("id", product.getId());
            formattedProduct.put("productId", product.getProductId());
            formattedProduct.put("title", product.getTitle());
            formattedProduct.put("description", product.getDescription());
            formattedProduct.put("weightUnit", product.getWeightUnit());
            formattedProduct.put("weight", product.getWeight());
            formattedProduct.put("price", product.getPrice());
            formattedProduct.put("pickupLocation", product.getPickupLocation());
            formattedProduct.put("state", product.getState());
            formattedProduct.put("district", product.getDistrict());
            formattedProduct.put("city", product.getCity());
            formattedProduct.put("locationLink", product.getLocationLink());
            formattedProduct.put("paymentOption", product.getPaymentOption());
            formattedProduct.put("paymentMethod", product.getPaymentMethod());
            formattedProduct.put("availableStock", product.getAvailableStock());
            formattedProduct.put("purchaseLimit", product.getPurchaseLimit());
            formattedProduct.put("active", product.getActive());
            formattedProduct.put("productType", product.getProductType());
            formattedProduct.put("productName", product.getProductName());
            formattedProduct.put("farmer", product.getFarmer());
            
            // Create images array from imageUrls collection
            List<Map<String, Object>> images = new ArrayList<>();
            
            // First check if there are any ProductImageUrl entries
            if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
                for (ProductImageUrl imageUrl : product.getImageUrls()) {
                    Map<String, Object> image = new HashMap<>();
                    image.put("id", imageUrl.getId());
                    image.put("imageUrl", imageUrl.getImageUrl());
                    image.put("displayOrder", imageUrl.getDisplayOrder());
                    images.add(image);
                }
            } 
            // If no ProductImageUrl entries, but there's a single imageUrl, add it to the images array
            else if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                Map<String, Object> image = new HashMap<>();
                image.put("id", 1); // Use a dummy ID
                image.put("imageUrl", product.getImageUrl());
                image.put("displayOrder", 1);
                images.add(image);
            }
            
            formattedProduct.put("images", images);
            
            return ResponseEntity.ok(formattedProduct);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Product not found"));
        }
    }
    
    /**
     * Get products by farmer ID
     */
    @GetMapping("/farmers/{farmerId}/products")
    public ResponseEntity<?> getProductsByFarmer(@PathVariable Long farmerId) {
        Optional<Farmer> farmerOpt = farmerRepository.findById(farmerId);
        
        if (farmerOpt.isPresent()) {
            // Filter products by active status and not deleted by admin
            List<Product> products = farmerOpt.get().getProducts().stream()
                    .filter(p -> p.getActive() && !p.getAdminDeleted())
                    .collect(Collectors.toList());
            
            // Format the response to include properly structured image arrays
            List<Map<String, Object>> formattedProducts = new ArrayList<>();
            
            for (Product product : products) {
                Map<String, Object> formattedProduct = new HashMap<>();
                // Copy all fields from product
                formattedProduct.put("id", product.getId());
                formattedProduct.put("productId", product.getProductId());
                formattedProduct.put("title", product.getTitle());
                formattedProduct.put("description", product.getDescription());
                formattedProduct.put("weightUnit", product.getWeightUnit());
                formattedProduct.put("weight", product.getWeight());
                formattedProduct.put("price", product.getPrice());
                formattedProduct.put("pickupLocation", product.getPickupLocation());
                formattedProduct.put("state", product.getState());
                formattedProduct.put("district", product.getDistrict());
                formattedProduct.put("city", product.getCity());
                formattedProduct.put("locationLink", product.getLocationLink());
                formattedProduct.put("paymentOption", product.getPaymentOption());
                formattedProduct.put("paymentMethod", product.getPaymentMethod());
                formattedProduct.put("availableStock", product.getAvailableStock());
                formattedProduct.put("purchaseLimit", product.getPurchaseLimit());
                formattedProduct.put("active", product.getActive());
                formattedProduct.put("productType", product.getProductType());
                formattedProduct.put("productName", product.getProductName());
                formattedProduct.put("farmer", product.getFarmer());
                
                // Create images array from imageUrls collection
                List<Map<String, Object>> images = new ArrayList<>();
                
                // First check if there are any ProductImageUrl entries
                if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
                    for (ProductImageUrl imageUrl : product.getImageUrls()) {
                        Map<String, Object> image = new HashMap<>();
                        image.put("id", imageUrl.getId());
                        image.put("imageUrl", imageUrl.getImageUrl());
                        image.put("displayOrder", imageUrl.getDisplayOrder());
                        images.add(image);
                    }
                } 
                // If no ProductImageUrl entries, but there's a single imageUrl, add it to the images array
                else if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                    Map<String, Object> image = new HashMap<>();
                    image.put("id", 1); // Use a dummy ID
                    image.put("imageUrl", product.getImageUrl());
                    image.put("displayOrder", 1);
                    images.add(image);
                }
                
                formattedProduct.put("images", images);
                
                formattedProducts.add(formattedProduct);
            }
            
            return ResponseEntity.ok(formattedProducts);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Farmer not found");
        }
    }
    
    /**
     * Get user profile
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("uniqueId", user.getUniqueId());
            response.put("username", user.getUsername());
            response.put("firstName", user.getFirstName());
            response.put("middleName", user.getMiddleName());
            response.put("lastName", user.getLastName());
            response.put("email", user.getEmail());
            response.put("phoneNumber", user.getPhoneNumber());
            response.put("dateOfBirth", user.getDateOfBirth());
            response.put("state", user.getState());
            response.put("district", user.getDistrict());
            response.put("city", user.getCity());
            response.put("locationLink", user.getLocationLink());
            response.put("userType", user.getUserType());
            
            // Add user type specific details
            if (user.getUserType().equals("FARMER")) {
                Optional<Farmer> farmerOpt = farmerRepository.findById(user.getId());
                if (farmerOpt.isPresent()) {
                    Farmer farmer = farmerOpt.get();
                    response.put("uidNumber", farmer.getUidNumber());
                }
            } else if (user.getUserType().equals("CONSUMER")) {
                // We don't need to use the consumer object right now, but we might add consumer-specific fields later
                // Just check if the consumer exists
                consumerRepository.findById(user.getId());
            }
            
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }
    
    /**
     * Get orders for a consumer
     */
    @GetMapping("/consumers/{consumerId}/orders")
    public ResponseEntity<?> getConsumerOrders(@PathVariable Long consumerId) {
        Optional<Consumer> consumerOpt = consumerRepository.findById(consumerId);
        
        if (consumerOpt.isPresent()) {
            List<Order> orders = orderRepository.findByConsumer(consumerOpt.get());
            return ResponseEntity.ok(orders);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Consumer not found");
        }
    }
    
    /**
     * Get order by ID
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> getOrderById(@PathVariable Long orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        
        if (orderOpt.isPresent()) {
            return ResponseEntity.ok(orderOpt.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found");
        }
    }
    
    /**
     * Get price limit for a product
     */
    @GetMapping("/price-limits")
    public ResponseEntity<Map<String, Object>> getPriceLimit(
            @RequestParam String productType,
            @RequestParam String productName,
            @RequestParam String weightUnit,
            @RequestParam Double weight) {
        
        Map<String, Object> response = new HashMap<>();
        
        // Calculate the maximum price based on the price limit
        Double maxPrice = priceLimitService.getMaxAllowedPrice(
            (String) productType,
            (String) productName,
            (String) weightUnit,
            (Double) weight
        );
        
        if (maxPrice != null) {
            response.put("maxPrice", maxPrice);
            response.put("productType", productType);
            response.put("productName", productName);
            response.put("weightUnit", weightUnit);
            response.put("weight", weight);
        } else {
            response.put("maxPrice", null);
            response.put("message", "No price limit found for this product");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get states and districts data
     */
    @GetMapping("/states-and-districts")
    public ResponseEntity<String> getStatesAndDistricts() {
        try {
            // Load the JSON file from the classpath
            Resource resource = 
                new ClassPathResource("data/states-and-districts.json");
            
            if (!resource.exists()) {
                // Try alternative path
                resource = new ClassPathResource("static/data/states-and-districts.json");
            }
            
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            String content = new String(resource.getInputStream().readAllBytes());
            
            return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(content);
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
    
    /**
     * Create a new product for a farmer
     */
    @PostMapping("/farmers/{farmerId}/products")
    public ResponseEntity<?> createProduct(@PathVariable Long farmerId, @RequestBody Map<String, Object> productRequest) {
        try {
            // Validate farmer
            Optional<Farmer> optionalFarmer = farmerRepository.findById(farmerId);
            if (!optionalFarmer.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Farmer not found with ID: " + farmerId));
            }
            
            Farmer farmer = optionalFarmer.get();
            
            // Validate required fields
            List<String> requiredFields = java.util.Arrays.asList(
                "title", "productType", "productName", "weight", "weightUnit", 
                "price", "pickupLocation", "state", "district", "city", 
                "availableStock", "purchaseLimit", "paymentMethod"
            );
            
            for (String field : requiredFields) {
                if (!productRequest.containsKey(field) || productRequest.get(field) == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Missing required field: " + field));
                }
            }
            
            // Create product
            Product product = new Product();
            product.setTitle((String) productRequest.get("title"));
            product.setDescription((String) productRequest.getOrDefault("description", ""));
            product.setProductType((String) productRequest.get("productType"));
            product.setProductName((String) productRequest.get("productName"));
            
            // Parse numeric values
            try {
                if (productRequest.get("weight") instanceof Number) {
                    product.setWeight(((Number) productRequest.get("weight")).doubleValue());
                } else {
                    product.setWeight(Double.parseDouble(productRequest.get("weight").toString()));
                }
                
                if (productRequest.get("price") instanceof Number) {
                    product.setPrice(((Number) productRequest.get("price")).doubleValue());
                } else {
                    product.setPrice(Double.parseDouble(productRequest.get("price").toString()));
                }
                
                if (productRequest.get("availableStock") instanceof Number) {
                    product.setAvailableStock(((Number) productRequest.get("availableStock")).intValue());
                } else {
                    product.setAvailableStock(Integer.parseInt(productRequest.get("availableStock").toString()));
                }
                
                if (productRequest.get("purchaseLimit") instanceof Number) {
                    product.setPurchaseLimit(((Number) productRequest.get("purchaseLimit")).intValue());
                } else {
                    product.setPurchaseLimit(Integer.parseInt(productRequest.get("purchaseLimit").toString()));
                }
            } catch (NumberFormatException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid numeric value: " + e.getMessage()));
            }
            
            // Check price limit if applicable
            if (priceLimitService != null) {
                Double maxPrice = priceLimitService.getMaxAllowedPrice(
                    (String) productRequest.get("productType"),
                    (String) productRequest.get("productName"),
                    (String) productRequest.get("weightUnit"),
                    (Double) product.getWeight()
                );
                
                if (maxPrice != null && product.getPrice() > maxPrice) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Price exceeds the maximum allowed price of ₹" + maxPrice));
                }
            }
            
            // Set other fields
            product.setWeightUnit((String) productRequest.get("weightUnit"));
            product.setPickupLocation((String) productRequest.get("pickupLocation"));
            product.setState((String) productRequest.get("state"));
            product.setDistrict((String) productRequest.get("district"));
            product.setCity((String) productRequest.get("city"));
            product.setLocationLink((String) productRequest.getOrDefault("locationLink", ""));
            
            // Set payment method and handle payment_option for database compatibility
            String paymentMethod = (String) productRequest.get("paymentMethod");
            product.setPaymentMethod(paymentMethod);
            // The paymentOption field will be automatically set in the setter method
            
            // Set multiple image URLs if provided
            if (productRequest.containsKey("imageUrls") && productRequest.get("imageUrls") != null) {
                try {
                    @SuppressWarnings("unchecked")
                    List<String> imageUrls = (List<String>) productRequest.get("imageUrls");
                    if (imageUrls != null && !imageUrls.isEmpty()) {
                        // Set the first image as the main image URL for backward compatibility
                        product.setImageUrl(imageUrls.get(0));
                        
                        // Add all image URLs to the product's imageUrls collection
                        for (int i = 0; i < imageUrls.size(); i++) {
                            ProductImageUrl productImageUrl = new ProductImageUrl(imageUrls.get(i), i + 1);
                            product.addImageUrl(productImageUrl);
                        }
                    }
                } catch (ClassCastException e) {
                    logger.error("Error casting imageUrls to List<String>: " + e.getMessage());
                }
            }
            
            // Set farmer and status
            product.setFarmer(farmer);
            product.setStatus("ACTIVE");
            product.setCreatedAt(new java.util.Date());
            
            // Generate a unique product ID
            String productId = "PROD-" + System.currentTimeMillis() + "-" + farmer.getId();
            product.setProductId(productId);
            
            // Save product
            Product savedProduct = productRepository.save(product);
            
            // Create a response with properly formatted images
            Map<String, Object> response = new HashMap<>();
            // Copy all fields from savedProduct
            response.put("id", savedProduct.getId());
            response.put("productId", savedProduct.getProductId());
            response.put("title", savedProduct.getTitle());
            response.put("description", savedProduct.getDescription());
            response.put("weightUnit", savedProduct.getWeightUnit());
            response.put("weight", savedProduct.getWeight());
            response.put("price", savedProduct.getPrice());
            response.put("pickupLocation", savedProduct.getPickupLocation());
            response.put("state", savedProduct.getState());
            response.put("district", savedProduct.getDistrict());
            response.put("city", savedProduct.getCity());
            response.put("locationLink", savedProduct.getLocationLink());
            response.put("paymentOption", savedProduct.getPaymentOption());
            response.put("paymentMethod", savedProduct.getPaymentMethod());
            response.put("availableStock", savedProduct.getAvailableStock());
            response.put("purchaseLimit", savedProduct.getPurchaseLimit());
            response.put("active", savedProduct.getActive());
            response.put("productType", savedProduct.getProductType());
            response.put("productName", savedProduct.getProductName());
            response.put("farmer", savedProduct.getFarmer());
            
            // Create images array from imageUrls collection
            List<Map<String, Object>> images = new ArrayList<>();
            for (ProductImageUrl imageUrl : savedProduct.getImageUrls()) {
                Map<String, Object> image = new HashMap<>();
                image.put("id", imageUrl.getId());
                image.put("imageUrl", imageUrl.getImageUrl());
                image.put("displayOrder", imageUrl.getDisplayOrder());
                images.add(image);
            }
            
            // If no image URLs were added but there's a single imageUrl, add it to the images array
            if (images.isEmpty() && savedProduct.getImageUrl() != null && !savedProduct.getImageUrl().isEmpty()) {
                Map<String, Object> image = new HashMap<>();
                image.put("id", 1); // Use a dummy ID for now
                image.put("imageUrl", savedProduct.getImageUrl());
                image.put("displayOrder", 1);
                images.add(image);
            }
            
            response.put("images", images);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Error creating product: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to create product: " + e.getMessage()));
        }
    }
    
    /**
     * Get product types and names
     */
    @GetMapping("/product-types")
    public ResponseEntity<?> getProductTypes() {
        try {
            // Read the type-and-product.json file
            Resource resource = new ClassPathResource("data/type-and-product.json");
            File file = resource.getFile();
            
            // Parse the JSON file
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(file);
            
            // Return the types array
            return ResponseEntity.ok(rootNode);
        } catch (Exception e) {
            logger.error("Error fetching product types: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch product types: " + e.getMessage()));
        }
    }
    
    /**
     * Upload a product image
     */
    @PostMapping("/farmers/{farmerId}/product-images")
    public ResponseEntity<?> uploadProductImage(
            @PathVariable Long farmerId,
            @RequestParam("image") MultipartFile image) {
        try {
            // Validate farmer
            Optional<Farmer> optionalFarmer = farmerRepository.findById(farmerId);
            if (!optionalFarmer.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Farmer not found with ID: " + farmerId));
            }
            
            // Validate image
            if (image.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Please select an image"));
            }
            
            // Check file type
            String contentType = image.getContentType();
            logger.info("Received image with content type: " + contentType);
            
            // Accept more image formats and be more lenient with content type checking
            // Also accept application/octet-stream which is common from Flutter
            if (contentType == null || 
                !(contentType.toLowerCase().contains("jpeg") || 
                  contentType.toLowerCase().contains("jpg") || 
                  contentType.toLowerCase().contains("png") || 
                  contentType.toLowerCase().contains("webp") ||
                  contentType.toLowerCase().equals("application/octet-stream"))) {
                
                logger.error("Invalid content type: " + contentType);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Only JPEG, JPG, PNG, and WEBP images are allowed. Received: " + contentType));
            }
            
            // Generate a unique filename
            String originalFilename = image.getOriginalFilename();
            String extension = ".jpg"; // Default extension
            
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            } else if (contentType != null) {
                // Try to determine extension from content type
                if (contentType.contains("jpeg") || contentType.contains("jpg")) {
                    extension = ".jpg";
                } else if (contentType.contains("png")) {
                    extension = ".png";
                } else if (contentType.contains("webp")) {
                    extension = ".webp";
                }
            }
            
            String filename = "product_" + System.currentTimeMillis() + extension;
            
            // Define the upload directory
            String uploadDir = "src/main/resources/static/uploads/products";
            Path uploadPath = Paths.get(uploadDir);
            
            // Create directories if they don't exist
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Save the file
            Path filePath = uploadPath.resolve(filename);
            Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Create the image URL
            String imageUrl = "/uploads/products/" + filename;
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("imageUrl", imageUrl));
        } catch (Exception e) {
            logger.error("Error uploading product image: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to upload image: " + e.getMessage()));
        }
    }
}

package com.example.demo.controller;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Consumer;
import com.example.demo.model.Farmer;
import com.example.demo.service.UserService;

@Controller
public class RegistrationController {

    @Autowired
    private UserService userService;
    
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }
    
    @GetMapping("/register/farmer")
    public String farmerRegistrationPage(Model model) {
        try {
            // Load the JSON file from the classpath
            org.springframework.core.io.Resource resource = 
                new org.springframework.core.io.ClassPathResource("data/states-and-districts.json");
            
            if (!resource.exists()) {
                // Try alternative path
                resource = new org.springframework.core.io.ClassPathResource("static/data/states-and-districts.json");
            }
            
            if (resource.exists()) {
                String jsonContent = new String(resource.getInputStream().readAllBytes());
                model.addAttribute("statesAndDistrictsJson", jsonContent);
            } else {
                model.addAttribute("jsonError", "JSON file not found");
            }
        } catch (Exception e) {
            model.addAttribute("jsonError", e.getMessage());
        }
        
        return "farmer-registration";
    }
    
    @GetMapping("/register/consumer")
    public String consumerRegistrationPage(Model model) {
        try {
            // Load the JSON file from the classpath
            org.springframework.core.io.Resource resource = 
                new org.springframework.core.io.ClassPathResource("data/states-and-districts.json");
            
            if (!resource.exists()) {
                // Try alternative path
                resource = new org.springframework.core.io.ClassPathResource("static/data/states-and-districts.json");
            }
            
            if (resource.exists()) {
                String jsonContent = new String(resource.getInputStream().readAllBytes());
                model.addAttribute("statesAndDistrictsJson", jsonContent);
            } else {
                model.addAttribute("jsonError", "JSON file not found");
            }
        } catch (Exception e) {
            model.addAttribute("jsonError", e.getMessage());
        }
        
        return "consumer-registration";
    }
    
    @PostMapping("/register/farmer")
    public String registerFarmer(
            @RequestParam String firstName,
            @RequestParam String middleName,
            @RequestParam String lastName,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam String phoneNumber,
            @RequestParam String email,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
            @RequestParam String state,
            @RequestParam String district,
            @RequestParam String city,
            @RequestParam String uidNumber,
            @RequestParam(required = false) String locationLink,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        // Validate input
        if (userService.isEmailTaken(email)) {
            model.addAttribute("error", "Email is already registered");
            return "farmer-registration";
        }
        
        if (userService.isPhoneNumberTaken(phoneNumber)) {
            model.addAttribute("error", "Phone number is already registered");
            return "farmer-registration";
        }
        
        if (userService.isUidNumberTaken(uidNumber)) {
            model.addAttribute("error", "UID number is already registered");
            return "farmer-registration";
        }
        
        if (userService.isUsernameTaken(username)) {
            model.addAttribute("error", "Username is already taken");
            return "farmer-registration";
        }
        
        // Check if passwords match
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match");
            return "farmer-registration";
        }
        
        // Create and save farmer
        Farmer farmer = new Farmer(null, username, password, firstName, middleName, lastName, phoneNumber, email, 
                dateOfBirth, state, district, city, locationLink, uidNumber);
        farmer.setUserType("FARMER");
        
        Farmer savedFarmer = userService.registerFarmer(farmer);
        
        redirectAttributes.addFlashAttribute("success", "Registration successful! Your account has been created.");
        redirectAttributes.addFlashAttribute("uniqueId", savedFarmer.getUniqueId());
        redirectAttributes.addFlashAttribute("username", savedFarmer.getUsername());
        return "redirect:/register/success";
    }
    
    @PostMapping("/register/consumer")
    public String registerConsumer(
            @RequestParam String firstName,
            @RequestParam String middleName,
            @RequestParam String lastName,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam String phoneNumber,
            @RequestParam String email,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
            @RequestParam String state,
            @RequestParam String district,
            @RequestParam String city,
            @RequestParam(required = false) String locationLink,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        // Validate input
        if (userService.isEmailTaken(email)) {
            model.addAttribute("error", "Email is already registered");
            return "consumer-registration";
        }
        
        if (userService.isPhoneNumberTaken(phoneNumber)) {
            model.addAttribute("error", "Phone number is already registered");
            return "consumer-registration";
        }
        
        if (userService.isUsernameTaken(username)) {
            model.addAttribute("error", "Username is already taken");
            return "consumer-registration";
        }
        
        // Check if passwords match
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match");
            return "consumer-registration";
        }
        
        // Create and save consumer
        Consumer consumer = new Consumer(null, username, password, firstName, middleName, lastName, phoneNumber, email, 
                dateOfBirth, state, district, city, locationLink);
        consumer.setUserType("CONSUMER");
        
        Consumer savedConsumer = userService.registerConsumer(consumer);
        
        redirectAttributes.addFlashAttribute("success", "Registration successful! Your account has been created.");
        redirectAttributes.addFlashAttribute("uniqueId", savedConsumer.getUniqueId());
        redirectAttributes.addFlashAttribute("username", savedConsumer.getUsername());
        return "redirect:/register/success";
    }
    
    @GetMapping("/register/success")
    public String registrationSuccess(Model model) {
        return "registration-success";
    }
    
    @GetMapping("/test-static-resources")
    public String testStaticResources(Model model) {
        try {
            // Try to load the JSON file from the classpath
            org.springframework.core.io.Resource resource = 
                new org.springframework.core.io.ClassPathResource("data/states-and-districts.json");
            
            if (!resource.exists()) {
                // Try alternative path
                resource = new org.springframework.core.io.ClassPathResource("static/data/states-and-districts.json");
            }
            
            boolean exists = resource.exists();
            boolean readable = resource.isReadable();
            String filename = resource.getFilename();
            String path = "Not available in JAR";
            
            try {
                path = resource.getURL().getPath();
            } catch (Exception e) {
                // URL might not be available in JAR
            }
            
            model.addAttribute("fileExists", exists);
            model.addAttribute("fileReadable", readable);
            model.addAttribute("filename", filename);
            model.addAttribute("path", path);
            
            if (exists) {
                try {
                    String content = new String(resource.getInputStream().readAllBytes());
                    model.addAttribute("fileContent", content.substring(0, Math.min(content.length(), 100)) + "...");
                } catch (Exception e) {
                    model.addAttribute("fileContentError", e.getMessage());
                }
            }
            
            return "test-resources";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "test-resources";
        }
    }
    
    @GetMapping("/api/states-and-districts")
    @ResponseBody
    public ResponseEntity<String> getStatesAndDistricts() {
        try {
            // Load the JSON file from the classpath
            org.springframework.core.io.Resource resource = 
                new org.springframework.core.io.ClassPathResource("data/states-and-districts.json");
            
            if (!resource.exists()) {
                // Try alternative path
                resource = new org.springframework.core.io.ClassPathResource("static/data/states-and-districts.json");
            }
            
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            String content = new String(resource.getInputStream().readAllBytes());
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(content);
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}

package com.example.demo.controller;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Consumer;
import com.example.demo.model.Farmer;
import com.example.demo.repository.ConsumerRepository;
import com.example.demo.repository.FarmerRepository;

@Controller
public class PasswordResetController {

    @Autowired
    private ConsumerRepository consumerRepository;

    @Autowired
    private FarmerRepository farmerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Store reset tokens with expiration time (30 minutes)
    private static final Map<String, ResetTokenInfo> resetTokens = new ConcurrentHashMap<>();

    private static class ResetTokenInfo {
        // These fields are used by the token validation logic
        private final String identifier;
        private final String userType;
        private final long expirationTime;

        public ResetTokenInfo(String identifier, String userType) {
            this.identifier = identifier;
            this.userType = userType;
            // Set expiration time to 30 minutes from now
            this.expirationTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(30);
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
        
        public String getIdentifier() {
            return identifier;
        }
        
        public String getUserType() {
            return userType;
        }
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage(@RequestParam(required = false) boolean success, Model model) {
        if (success) {
            model.addAttribute("message", "Password reset link has been sent to your email. Please check your inbox.");
            model.addAttribute("messageType", "success");
        }
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(
            @RequestParam String identifier,
            @RequestParam String userType,
            RedirectAttributes redirectAttributes) {

        // Check if the user exists
        boolean userExists = false;
        String email = identifier;
        
        if ("CONSUMER".equals(userType)) {
            Consumer consumer = consumerRepository.findByEmailOrUsernameOrPhoneNumber(identifier).orElse(null);
            if (consumer != null) {
                userExists = true;
                // If identifier is not an email, use the consumer's email
                if (!identifier.contains("@")) {
                    email = consumer.getEmail();
                }
            }
        } else if ("FARMER".equals(userType)) {
            Farmer farmer = farmerRepository.findByEmailOrUsernameOrPhoneNumber(identifier).orElse(null);
            if (farmer != null) {
                userExists = true;
                // If identifier is not an email, use the farmer's email
                if (!identifier.contains("@")) {
                    email = farmer.getEmail();
                }
            }
        }

        if (!userExists) {
            redirectAttributes.addFlashAttribute("message", "No account found with the provided information.");
            redirectAttributes.addFlashAttribute("messageType", "danger");
            return "redirect:/forgot-password";
        }

        // Generate a unique token
        String token = UUID.randomUUID().toString();
        resetTokens.put(token, new ResetTokenInfo(identifier, userType));

        // Create reset link
        String resetLink = "http://localhost:8080/reset-password?token=" + token + "&identifier=" + identifier + "&userType=" + userType;
        
        // Pass the reset information to the view for client-side email sending
        redirectAttributes.addFlashAttribute("sendEmail", true);
        redirectAttributes.addFlashAttribute("resetEmail", email);
        redirectAttributes.addFlashAttribute("resetLink", resetLink);
        redirectAttributes.addFlashAttribute("resetUserType", userType);
        
        // Add a message to inform the user that the process is starting
        redirectAttributes.addFlashAttribute("message", "We found your account. Preparing to send a password reset link to your email...");
        redirectAttributes.addFlashAttribute("messageType", "info");
        
        // Also log the link for development purposes
        System.out.println("Reset Link: " + resetLink);
        
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordPage(
            @RequestParam String token,
            @RequestParam(required = false) String identifier,
            @RequestParam(required = false) String userType,
            Model model) {

        // Check if token exists and is valid
        ResetTokenInfo tokenInfo = resetTokens.get(token);
        if (tokenInfo == null || tokenInfo.isExpired()) {
            model.addAttribute("invalidToken", true);
            return "reset-password";
        }
        
        // If identifier or userType is null, use the values from tokenInfo
        if (identifier == null || userType == null) {
            identifier = tokenInfo.getIdentifier();
            userType = tokenInfo.getUserType();
        }
        
        model.addAttribute("token", token);
        model.addAttribute("identifier", identifier);
        model.addAttribute("userType", userType);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(
            @RequestParam String token,
            @RequestParam String identifier,
            @RequestParam String userType,
            @RequestParam String newPassword,
            RedirectAttributes redirectAttributes) {

        // Check if token exists and is valid
        ResetTokenInfo tokenInfo = resetTokens.get(token);
        if (tokenInfo == null || tokenInfo.isExpired()) {
            redirectAttributes.addFlashAttribute("message", "This password reset link is invalid or has expired.");
            redirectAttributes.addFlashAttribute("messageType", "danger");
            return "redirect:/forgot-password";
        }

        // Encode the new password
        String encodedPassword = passwordEncoder.encode(newPassword);

        // Update the user's password
        boolean passwordUpdated = false;
        if ("CONSUMER".equals(userType)) {
            Consumer consumer = consumerRepository.findByEmailOrUsernameOrPhoneNumber(identifier).orElse(null);
            if (consumer != null) {
                consumer.setPassword(encodedPassword);
                consumerRepository.save(consumer);
                passwordUpdated = true;
            }
        } else if ("FARMER".equals(userType)) {
            Farmer farmer = farmerRepository.findByEmailOrUsernameOrPhoneNumber(identifier).orElse(null);
            if (farmer != null) {
                farmer.setPassword(encodedPassword);
                farmerRepository.save(farmer);
                passwordUpdated = true;
            }
        }

        if (!passwordUpdated) {
            redirectAttributes.addFlashAttribute("message", "Failed to update password. User not found.");
            redirectAttributes.addFlashAttribute("messageType", "danger");
            return "redirect:/forgot-password";
        }

        // Remove the used token
        resetTokens.remove(token);

        // Redirect to login page with success message
        redirectAttributes.addFlashAttribute("message", "Your password has been successfully reset. You can now login with your new password.");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/login?passwordReset=true";
    }
}

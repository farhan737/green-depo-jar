package com.example.demo.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // Check if user is authenticated and not anonymous
        if (authentication != null && authentication.isAuthenticated() && 
            !authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            
            // Redirect based on user role
            if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_FARMER"))) {
                return "redirect:/farmer-dashboard";
            } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CONSUMER"))) {
                return "redirect:/consumer-dashboard";
            }
        }
        
        // If not authenticated or no specific role, show the home page
        return "index";
    }
}

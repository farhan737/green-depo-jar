package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);
    
    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        logger.info("Attempting to load user with login: {}", login);
        
        // Try to find user by username, unique ID, or phone number
        Optional<User> userOpt = userRepository.findByUsernameOrUniqueIdOrPhoneNumber(login, login, login);
        
        if (!userOpt.isPresent()) {
            logger.error("User not found with login: {}", login);
            throw new UsernameNotFoundException("User not found with login: " + login);
        }
        
        User user = userOpt.get();
        
        // Get userType from session if available
        String expectedUserType = null;
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpSession session = attr.getRequest().getSession(false);
            if (session != null) {
                expectedUserType = (String) session.getAttribute("userType");
            }
        } catch (Exception e) {
            logger.warn("Could not access session to get userType", e);
        }
        
        // If expectedUserType is set, check if user has the correct type
        if (expectedUserType != null && !expectedUserType.equals(user.getUserType())) {
            logger.error("User type mismatch. Expected: {}, Found: {}", expectedUserType, user.getUserType());
            throw new UsernameNotFoundException("Invalid credentials for user type: " + expectedUserType);
        }
        
        logger.info("Found user: {}, userType: {}", user.getUsername(), user.getUserType());
        
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // Add role based on user type
        if ("FARMER".equals(user.getUserType())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_FARMER"));
        } else if ("CONSUMER".equals(user.getUserType())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_CONSUMER"));
        } else if ("ADMIN".equals(user.getUserType())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        
        logger.info("User authenticated successfully: {}", user.getUsername());
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), 
                user.getPassword(), 
                authorities);
    }
}

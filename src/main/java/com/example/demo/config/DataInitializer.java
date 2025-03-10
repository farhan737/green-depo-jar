package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.UserService;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public void run(String... args) throws Exception {
        // Check if admin user exists
        if (!userRepository.existsByUsernameAndUserType("admin", "ADMIN")) {
            logger.info("Creating admin user");
            User admin = userService.registerAdmin("admin", "Farhan@93933");
            logger.info("Admin user created with uniqueId: {}", admin.getUniqueId());
        } else {
            logger.info("Admin user already exists");
        }
    }
}

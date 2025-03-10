package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.User;
import com.example.demo.service.UserService;

@RestController
public class TestController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @GetMapping("/test/user")
    public String testUser(@RequestParam String login) {
        User user = userService.findUserByLoginIdentifier(login);
        if (user == null) {
            return "User not found with login: " + login;
        }
        return "User found: " + user.getUsername() + ", Type: " + user.getUserType();
    }
    
    @GetMapping("/test/password")
    public String testPassword(@RequestParam String login, @RequestParam String password) {
        User user = userService.findUserByLoginIdentifier(login);
        if (user == null) {
            return "User not found with login: " + login;
        }
        
        boolean matches = passwordEncoder.matches(password, user.getPassword());
        return "Password match: " + matches + " for user: " + user.getUsername() + 
                "\nStored hash: " + user.getPassword() + 
                "\nTest hash: " + passwordEncoder.encode(password);
    }
    
    @GetMapping("/test/encode")
    public String encodePassword(@RequestParam String password) {
        String encoded = passwordEncoder.encode(password);
        return "Original: " + password + "\nEncoded: " + encoded;
    }
}

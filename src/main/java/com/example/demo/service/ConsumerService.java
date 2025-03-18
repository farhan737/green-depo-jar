package com.example.demo.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Consumer;
import com.example.demo.model.User;
import com.example.demo.repository.ConsumerRepository;
import com.example.demo.repository.UserRepository;

@Service
public class ConsumerService {

    @Autowired
    private ConsumerRepository consumerRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get a consumer by their username
     * 
     * @param username The username of the consumer
     * @return The consumer with the given username
     */
    public Consumer getConsumerByUsername(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent() && userOpt.get() instanceof Consumer) {
            return (Consumer) userOpt.get();
        }
        return null;
    }
    
    /**
     * Get a consumer by their ID
     * 
     * @param id The ID of the consumer
     * @return The consumer with the given ID
     */
    public Consumer getConsumerById(Long id) {
        return consumerRepository.findById(id).orElse(null);
    }
}

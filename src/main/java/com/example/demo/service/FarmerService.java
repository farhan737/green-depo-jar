package com.example.demo.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Farmer;
import com.example.demo.model.User;
import com.example.demo.repository.FarmerRepository;
import com.example.demo.repository.UserRepository;

@Service
public class FarmerService {

    @Autowired
    private FarmerRepository farmerRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get a farmer by their username
     * 
     * @param username The username of the farmer
     * @return The farmer with the given username
     */
    public Farmer getFarmerByUsername(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent() && userOpt.get() instanceof Farmer) {
            return (Farmer) userOpt.get();
        }
        return null;
    }
    
    /**
     * Get a farmer by their ID
     * 
     * @param id The ID of the farmer
     * @return The farmer with the given ID
     */
    public Farmer getFarmerById(Long id) {
        return farmerRepository.findById(id).orElse(null);
    }
}

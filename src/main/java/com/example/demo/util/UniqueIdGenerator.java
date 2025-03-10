package com.example.demo.util;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * Utility class for generating unique 8-digit alphanumeric IDs
 */
@Component
public class UniqueIdGenerator {
    
    private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int ID_LENGTH = 8;
    private static final SecureRandom random = new SecureRandom();
    
    // Keep track of generated IDs to avoid duplicates (in-memory cache)
    private static final Set<String> generatedIds = new HashSet<>();
    
    /**
     * Generates a unique 8-digit alphanumeric ID
     * @return a unique ID string
     */
    public String generateUniqueId() {
        String uniqueId;
        do {
            uniqueId = generateRandomAlphanumeric();
        } while (generatedIds.contains(uniqueId));
        
        // Add to the set of generated IDs
        generatedIds.add(uniqueId);
        return uniqueId;
    }
    
    /**
     * Generates a random alphanumeric string of length ID_LENGTH
     * @return a random alphanumeric string
     */
    private String generateRandomAlphanumeric() {
        StringBuilder builder = new StringBuilder(ID_LENGTH);
        for (int i = 0; i < ID_LENGTH; i++) {
            int index = random.nextInt(ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(index));
        }
        return builder.toString();
    }
}

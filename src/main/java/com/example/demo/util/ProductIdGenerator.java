package com.example.demo.util;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class ProductIdGenerator {
    
    private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int PRODUCT_ID_LENGTH = 8;
    private static final SecureRandom random = new SecureRandom();
    
    public String generateProductId() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < PRODUCT_ID_LENGTH; i++) {
            int index = random.nextInt(ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(index));
        }
        return builder.toString();
    }
}

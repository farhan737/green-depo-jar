package com.example.demo.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "consumers")
public class Consumer extends User {
    
    @OneToMany(mappedBy = "consumer")
    @JsonManagedReference("consumer-orders")
    private List<Order> orders = new ArrayList<>();
    
    // Constructors
    public Consumer() {
        super();
        setUserType("CONSUMER");
    }
    
    public Consumer(String uniqueId, String username, String password, String firstName, String middleName, String lastName, String phoneNumber, String email,
            LocalDate dateOfBirth, String state, String district, String city, String locationLink) {
        super(uniqueId, username, password, firstName, middleName, lastName, phoneNumber, email, dateOfBirth, state, district, city, locationLink, "CONSUMER");
    }
    
    // Getters and Setters
    public List<Order> getOrders() {
        return orders;
    }
    
    public void setOrders(List<Order> orders) {
        this.orders = orders;
    }
}

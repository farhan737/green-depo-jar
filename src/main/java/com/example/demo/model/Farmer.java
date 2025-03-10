package com.example.demo.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "farmers")
public class Farmer extends User {
    
    @Column(nullable = false, unique = true)
    private String uidNumber;
    
    @OneToMany(mappedBy = "farmer")
    @JsonManagedReference("farmer-products")
    private List<Product> products = new ArrayList<>();
    
    // Constructors
    public Farmer() {
        super();
        setUserType("FARMER");
    }
    
    public Farmer(String uniqueId, String username, String password, String firstName, String middleName, String lastName, String phoneNumber, String email,
            LocalDate dateOfBirth, String state, String district, String city, String locationLink, String uidNumber) {
        super(uniqueId, username, password, firstName, middleName, lastName, phoneNumber, email, dateOfBirth, state, district, city, locationLink, "FARMER");
        this.uidNumber = uidNumber;
    }
    
    // Getters and Setters
    public String getUidNumber() {
        return uidNumber;
    }
    
    public void setUidNumber(String uidNumber) {
        this.uidNumber = uidNumber;
    }
    
    public List<Product> getProducts() {
        return products;
    }
    
    public void setProducts(List<Product> products) {
        this.products = products;
    }
}

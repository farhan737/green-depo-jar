package com.example.demo.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String productId;
    
    @Column(nullable = false)
    private String title;
    
    @Column(length = 1000)
    private String description;
    
    @Column(nullable = false)
    private String weightUnit; // "g" or "kg"
    
    @Column(nullable = false)
    private Double weight;
    
    @Column(nullable = false)
    private Double price;
    
    @Column(nullable = false)
    private String pickupLocation;
    
    @Column
    private String state;
    
    @Column
    private String district;
    
    @Column
    private String city;
    
    @Column(nullable = false, length = 1000)
    private String locationLink;
    
    @Column(nullable = false)
    private String paymentOption; // "on-pickup" or "pre-pickup"
    
    @Column(nullable = false)
    private Integer availableStock = 0; // Total available stock units
    
    @Column(nullable = false)
    private Integer purchaseLimit = 0; // Maximum units a consumer can purchase
    
    @Column(nullable = false)
    private Boolean active = true; // Whether the product is active/available
    
    @Column(nullable = false)
    private Boolean adminDeleted = false; // Whether the product is deleted by admin
    
    @Column
    private String productType; // New field for product type (e.g., "Cereals", "Pulses")
    
    @Column
    private String productName; // New field for specific product (e.g., "Wheat", "Rice")
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("product-images")
    private List<ProductImage> images = new ArrayList<>();
    
    @OneToMany(mappedBy = "product")
    @JsonManagedReference("product-orderItems")
    private List<OrderItem> orderItems = new ArrayList<>();
    
    @ManyToOne
    @JoinColumn(name = "farmer_id", nullable = false)
    @JsonBackReference("farmer-products")
    private Farmer farmer;
    
    // Constructors
    public Product() {
    }

    public Product(String productId, String title, String description, String weightUnit, Double weight, Double price,
                  String pickupLocation, String state, String district, String city, String locationLink,
                  String paymentOption, Integer availableStock, Integer purchaseLimit, Boolean active, Boolean adminDeleted, String productType, String productName, Farmer farmer) {
        this.productId = productId;
        this.title = title;
        this.description = description;
        this.weightUnit = weightUnit;
        this.weight = weight;
        this.price = price;
        this.pickupLocation = pickupLocation;
        this.state = state;
        this.district = district;
        this.city = city;
        this.locationLink = locationLink;
        this.paymentOption = paymentOption;
        this.availableStock = availableStock;
        this.purchaseLimit = purchaseLimit;
        this.active = active;
        this.adminDeleted = adminDeleted;
        this.productType = productType;
        this.productName = productName;
        this.farmer = farmer;
    }

    // Helper method to add an image
    public void addImage(ProductImage image) {
        images.add(image);
        image.setProduct(this);
    }
    
    // Helper method to remove an image
    public void removeImage(ProductImage image) {
        images.remove(image);
        image.setProduct(null);
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWeightUnit() {
        return weightUnit;
    }

    public void setWeightUnit(String weightUnit) {
        this.weightUnit = weightUnit;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getLocationLink() {
        return locationLink;
    }

    public void setLocationLink(String locationLink) {
        this.locationLink = locationLink;
    }

    public String getPaymentOption() {
        return paymentOption;
    }

    public void setPaymentOption(String paymentOption) {
        this.paymentOption = paymentOption;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getAdminDeleted() {
        return adminDeleted;
    }

    public void setAdminDeleted(Boolean adminDeleted) {
        this.adminDeleted = adminDeleted;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
    }

    public Integer getPurchaseLimit() {
        return purchaseLimit;
    }

    public void setPurchaseLimit(Integer purchaseLimit) {
        this.purchaseLimit = purchaseLimit;
    }
    
    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public List<ProductImage> getImages() {
        // Sort images by displayOrder if it's not null
        images.sort((img1, img2) -> {
            Integer order1 = img1.getDisplayOrder();
            Integer order2 = img2.getDisplayOrder();
            
            // Handle null display orders
            if (order1 == null && order2 == null) {
                return 0;
            } else if (order1 == null) {
                return 1;
            } else if (order2 == null) {
                return -1;
            }
            
            return order1.compareTo(order2);
        });
        return images;
    }

    public void setImages(List<ProductImage> images) {
        this.images = images;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    public Farmer getFarmer() {
        return farmer;
    }

    public void setFarmer(Farmer farmer) {
        this.farmer = farmer;
    }
}

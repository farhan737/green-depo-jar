package com.example.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "post_reactions", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"}))
public class PostReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false)
    private FarmerPost post;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "user_type", nullable = false)
    private String userType; // "FARMER" or "CONSUMER"
    
    @Column(name = "is_like", nullable = false)
    private boolean positive; // true for like, false for dislike
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    // Default constructor
    public PostReaction() {
        this.createdAt = LocalDateTime.now();
    }
    
    // Constructor with fields
    public PostReaction(FarmerPost post, Long userId, String userType, boolean positive) {
        this.post = post;
        this.userId = userId;
        this.userType = userType;
        this.positive = positive;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FarmerPost getPost() {
        return post;
    }

    public void setPost(FarmerPost post) {
        this.post = post;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public boolean isPositive() {
        return positive;
    }

    public void setPositive(boolean positive) {
        this.positive = positive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

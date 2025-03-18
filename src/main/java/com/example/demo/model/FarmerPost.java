package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "farmer_posts")
public class FarmerPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false, length = 1000)
    private String description;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "farmer_id", nullable = false)
    private Farmer farmer;
    
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostImage> images = new ArrayList<>();
    
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostReaction> reactions = new ArrayList<>();
    
    // Calculated fields
    @Column
    private int likeCount = 0;
    
    @Column
    private int dislikeCount = 0;
    
    // Default constructor
    public FarmerPost() {
        this.createdAt = LocalDateTime.now();
    }
    
    // Constructor with fields
    public FarmerPost(String title, String description, Farmer farmer) {
        this.title = title;
        this.description = description;
        this.farmer = farmer;
        this.createdAt = LocalDateTime.now();
    }
    
    // Helper method to add an image
    public void addImage(PostImage image) {
        images.add(image);
        image.setPost(this);
    }
    
    // Helper method to remove an image
    public void removeImage(PostImage image) {
        images.remove(image);
        image.setPost(null);
    }
    
    // Helper method to add a reaction
    public void addReaction(PostReaction reaction) {
        reactions.add(reaction);
        reaction.setPost(this);
        
        // Update reaction counts
        if (reaction.isPositive()) {
            likeCount++;
        } else {
            dislikeCount++;
        }
    }
    
    // Helper method to remove a reaction
    public void removeReaction(PostReaction reaction) {
        reactions.remove(reaction);
        reaction.setPost(null);
        
        // Update reaction counts
        if (reaction.isPositive()) {
            likeCount--;
        } else {
            dislikeCount--;
        }
    }
    
    // Helper method to update reaction counts
    public void updateReactionCounts() {
        this.likeCount = (int) reactions.stream().filter(PostReaction::isPositive).count();
        this.dislikeCount = (int) reactions.stream().filter(r -> !r.isPositive()).count();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Farmer getFarmer() {
        return farmer;
    }

    public void setFarmer(Farmer farmer) {
        this.farmer = farmer;
    }

    public List<PostImage> getImages() {
        return images;
    }

    public void setImages(List<PostImage> images) {
        this.images = images;
    }

    public List<PostReaction> getReactions() {
        return reactions;
    }

    public void setReactions(List<PostReaction> reactions) {
        this.reactions = reactions;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getDislikeCount() {
        return dislikeCount;
    }

    public void setDislikeCount(int dislikeCount) {
        this.dislikeCount = dislikeCount;
    }
    
    // Total reactions count
    public int getTotalReactions() {
        return likeCount + dislikeCount;
    }
}

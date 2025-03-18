package com.example.demo.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.Farmer;
import com.example.demo.model.FarmerPost;
import com.example.demo.model.PostImage;
import com.example.demo.model.PostReaction;
import com.example.demo.repository.FarmerPostRepository;
import com.example.demo.repository.PostReactionRepository;

@Service
public class FarmerPostService {

    @Autowired
    private FarmerPostRepository farmerPostRepository;
    
    @Autowired
    private PostReactionRepository postReactionRepository;
    
    private final Path postImagesPath = Paths.get("src/main/resources/static/post-images");
    
    // Initialize the directory for post images
    public void init() {
        try {
            if (!Files.exists(postImagesPath)) {
                Files.createDirectories(postImagesPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize folder for post images", e);
        }
    }
    
    // Create a new post with images
    @Transactional
    public FarmerPost createPost(String title, String description, Farmer farmer, List<MultipartFile> images) {
        // Create and save the post
        FarmerPost post = new FarmerPost(title, description, farmer);
        farmerPostRepository.save(post);
        
        // Process and save images (up to 3)
        if (images != null && !images.isEmpty()) {
            int imageCount = 0;
            for (MultipartFile image : images) {
                if (!image.isEmpty() && imageCount < 3) {
                    try {
                        // Generate a unique filename
                        String filename = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
                        Path targetPath = postImagesPath.resolve(filename);
                        
                        // Save the file
                        Files.copy(image.getInputStream(), targetPath);
                        
                        // Create and save the image entity
                        PostImage postImage = new PostImage("/post-images/" + filename);
                        post.addImage(postImage);
                        
                        imageCount++;
                    } catch (IOException e) {
                        throw new RuntimeException("Could not store the image", e);
                    }
                }
            }
        }
        
        // Save the post with images
        return farmerPostRepository.save(post);
    }
    
    // Get all posts
    public List<FarmerPost> getAllPosts(String sortBy) {
        switch (sortBy) {
            case "reactions":
                return farmerPostRepository.findAllOrderByTotalReactionsDesc();
            case "likes":
                return farmerPostRepository.findAllByOrderByLikeCountDesc();
            case "dislikes":
                return farmerPostRepository.findAllByOrderByDislikeCountDesc();
            default:
                return farmerPostRepository.findAllByOrderByCreatedAtDesc();
        }
    }
    
    // Get posts by farmer
    public List<FarmerPost> getPostsByFarmer(Long farmerId) {
        return farmerPostRepository.findByFarmerIdOrderByCreatedAtDesc(farmerId);
    }
    
    // Get a post by ID
    public Optional<FarmerPost> getPostById(Long postId) {
        return farmerPostRepository.findById(postId);
    }
    
    // Delete a post
    @Transactional
    public void deletePost(Long postId) {
        Optional<FarmerPost> postOpt = farmerPostRepository.findById(postId);
        
        if (postOpt.isPresent()) {
            FarmerPost post = postOpt.get();
            
            // Delete associated images from filesystem
            for (PostImage image : post.getImages()) {
                try {
                    String filename = image.getImagePath().substring(image.getImagePath().lastIndexOf('/') + 1);
                    Files.deleteIfExists(postImagesPath.resolve(filename));
                } catch (IOException e) {
                    // Log error but continue with deletion
                    System.err.println("Error deleting image file: " + e.getMessage());
                }
            }
            
            // Delete the post (cascades to images and reactions)
            farmerPostRepository.delete(post);
        }
    }
    
    // Add or update a reaction (like/dislike)
    @Transactional
    public void reactToPost(Long postId, Long userId, String userType, boolean isLike) {
        Optional<FarmerPost> postOpt = farmerPostRepository.findById(postId);
        
        if (postOpt.isPresent()) {
            FarmerPost post = postOpt.get();
            
            // Check if user already reacted to this post
            Optional<PostReaction> existingReaction = 
                postReactionRepository.findByPostIdAndUserIdAndUserType(postId, userId, userType);
            
            if (existingReaction.isPresent()) {
                PostReaction reaction = existingReaction.get();
                
                // If reaction type is the same, remove it (toggle off)
                if (reaction.isPositive() == isLike) {
                    post.removeReaction(reaction);
                    postReactionRepository.delete(reaction);
                } else {
                    // Update the reaction type
                    reaction.setPositive(isLike);
                    postReactionRepository.save(reaction);
                    
                    // Update reaction counts
                    post.updateReactionCounts();
                }
            } else {
                // Create a new reaction
                PostReaction reaction = new PostReaction(post, userId, userType, isLike);
                post.addReaction(reaction);
                postReactionRepository.save(reaction);
            }
            
            // Save the updated post
            farmerPostRepository.save(post);
        }
    }
    
    // Check if a user has reacted to a post
    public Optional<PostReaction> getUserReaction(Long postId, Long userId, String userType) {
        return postReactionRepository.findByPostIdAndUserIdAndUserType(postId, userId, userType);
    }
}

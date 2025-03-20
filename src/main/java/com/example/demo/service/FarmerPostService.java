package com.example.demo.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

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
    
    // Initialize method no longer needs to create a directory
    public void init() {
        // No need to initialize a directory anymore
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
                        // Get image data
                        String filename = image.getOriginalFilename();
                        String contentType = image.getContentType();
                        byte[] data = image.getBytes();
                        
                        // Create and save the image entity
                        PostImage postImage = new PostImage(filename, contentType, data);
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

package com.example.demo.controller;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Consumer;
import com.example.demo.model.Farmer;
import com.example.demo.model.FarmerPost;
import com.example.demo.model.PostReaction;
import com.example.demo.service.ConsumerService;
import com.example.demo.service.FarmerPostService;
import com.example.demo.service.FarmerService;

import jakarta.annotation.PostConstruct;

@Controller
public class FarmerPostController {

    @Autowired
    private FarmerPostService farmerPostService;
    
    @Autowired
    private FarmerService farmerService;
    
    @Autowired
    private ConsumerService consumerService;
    
    @PostConstruct
    public void init() {
        farmerPostService.init();
    }
    
    // Farmer's posts page (your posts)
    @GetMapping("/farmer/posts")
    public String farmerPosts(Model model, Principal principal) {
        Farmer farmer = farmerService.getFarmerByUsername(principal.getName());
        List<FarmerPost> posts = farmerPostService.getPostsByFarmer(farmer.getId());
        
        model.addAttribute("farmer", farmer);
        model.addAttribute("posts", posts);
        
        return "farmer-posts";
    }
    
    // Create a new post
    @PostMapping("/farmer/posts/create")
    public String createPost(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam(required = false) List<MultipartFile> images,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        Farmer farmer = farmerService.getFarmerByUsername(principal.getName());
        
        try {
            farmerPostService.createPost(title, description, farmer, images);
            redirectAttributes.addFlashAttribute("successMessage", "Post created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating post: " + e.getMessage());
        }
        
        return "redirect:/farmer/posts";
    }
    
    // Delete a post
    @PostMapping("/farmer/posts/delete/{postId}")
    public String deletePost(@PathVariable Long postId, Principal principal, RedirectAttributes redirectAttributes) {
        Farmer farmer = farmerService.getFarmerByUsername(principal.getName());
        Optional<FarmerPost> postOpt = farmerPostService.getPostById(postId);
        
        if (postOpt.isPresent()) {
            FarmerPost post = postOpt.get();
            
            // Check if the post belongs to the current farmer
            if (post.getFarmer().getId().equals(farmer.getId())) {
                farmerPostService.deletePost(postId);
                redirectAttributes.addFlashAttribute("successMessage", "Post deleted successfully!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "You can only delete your own posts!");
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Post not found!");
        }
        
        return "redirect:/farmer/posts";
    }
    
    // All posts page for farmers
    @GetMapping("/farmer/all-posts")
    public String allPostsForFarmer(
            @RequestParam(defaultValue = "recent") String sortBy,
            Model model, 
            Principal principal) {
        
        Farmer farmer = farmerService.getFarmerByUsername(principal.getName());
        List<FarmerPost> posts = farmerPostService.getAllPosts(sortBy);
        
        model.addAttribute("posts", posts);
        model.addAttribute("currentUser", farmer);
        model.addAttribute("userType", "FARMER");
        model.addAttribute("sortBy", sortBy);
        
        // Check user reactions for each post
        for (FarmerPost post : posts) {
            Optional<PostReaction> reaction = farmerPostService.getUserReaction(
                    post.getId(), farmer.getId(), "FARMER");
            
            if (reaction.isPresent()) {
                model.addAttribute("reaction_" + post.getId(), 
                        reaction.get().isPositive() ? "like" : "dislike");
            }
        }
        
        return "all-posts";
    }
    
    // All posts page for consumers
    @GetMapping("/consumer/all-posts")
    public String allPostsForConsumer(
            @RequestParam(defaultValue = "recent") String sortBy,
            Model model, 
            Principal principal) {
        
        Consumer consumer = consumerService.getConsumerByUsername(principal.getName());
        List<FarmerPost> posts = farmerPostService.getAllPosts(sortBy);
        
        model.addAttribute("posts", posts);
        model.addAttribute("currentUser", consumer);
        model.addAttribute("userType", "CONSUMER");
        model.addAttribute("sortBy", sortBy);
        
        // Check user reactions for each post
        for (FarmerPost post : posts) {
            Optional<PostReaction> reaction = farmerPostService.getUserReaction(
                    post.getId(), consumer.getId(), "CONSUMER");
            
            if (reaction.isPresent()) {
                model.addAttribute("reaction_" + post.getId(), 
                        reaction.get().isPositive() ? "like" : "dislike");
            }
        }
        
        return "all-posts";
    }
    
    // React to a post (like/dislike)
    @PostMapping("/api/posts/{postId}/react")
    @ResponseBody
    public ResponseEntity<?> reactToPost(
            @PathVariable Long postId,
            @RequestParam(name = "isLike") boolean positive,
            Principal principal,
            @RequestParam String userType) {
        
        Long userId;
        
        if ("FARMER".equals(userType)) {
            Farmer farmer = farmerService.getFarmerByUsername(principal.getName());
            userId = farmer.getId();
        } else if ("CONSUMER".equals(userType)) {
            Consumer consumer = consumerService.getConsumerByUsername(principal.getName());
            userId = consumer.getId();
        } else {
            return ResponseEntity.badRequest().body("Invalid user type");
        }
        
        try {
            farmerPostService.reactToPost(postId, userId, userType, positive);
            Optional<FarmerPost> postOpt = farmerPostService.getPostById(postId);
            
            if (postOpt.isPresent()) {
                FarmerPost post = postOpt.get();
                return ResponseEntity.ok().body(
                        "{\"likeCount\": " + post.getLikeCount() + 
                        ", \"dislikeCount\": " + post.getDislikeCount() + "}");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}

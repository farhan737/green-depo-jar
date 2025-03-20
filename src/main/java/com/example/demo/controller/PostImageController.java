package com.example.demo.controller;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.demo.model.FarmerPost;
import com.example.demo.model.PostImage;
import com.example.demo.repository.PostImageRepository;
import com.example.demo.service.FarmerPostService;

@Controller
public class PostImageController {
    
    private static final Logger logger = LoggerFactory.getLogger(PostImageController.class);

    @Autowired
    private PostImageRepository postImageRepository;
    
    @Autowired
    private FarmerPostService farmerPostService;
    
    @GetMapping("/post-image/{id}")
    public ResponseEntity<byte[]> getPostImage(@PathVariable Long id) {
        logger.info("Fetching post image with ID: {}", id);
        
        try {
            Optional<PostImage> imageOptional = postImageRepository.findById(id);
            
            if (!imageOptional.isPresent()) {
                logger.error("Post image not found with ID: {}", id);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            
            PostImage image = imageOptional.get();
            logger.info("Found post image: {} of type: {}", image.getFileName(), image.getContentType());
            
            HttpHeaders headers = new HttpHeaders();
            
            // Set content type based on the stored content type
            headers.setContentType(MediaType.parseMediaType(image.getContentType()));
            
            // Set filename for download
            headers.setContentDispositionFormData("inline", image.getFileName());
            
            return new ResponseEntity<>(image.getData(), headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving post image with ID: {}", id, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/post-image/{postId}/{index}")
    public ResponseEntity<byte[]> getPostImageByIndex(
            @PathVariable Long postId, 
            @PathVariable Integer index) {
        
        logger.info("Fetching image for post ID: {} at index: {}", postId, index);
        
        try {
            // Find the post by postId
            Optional<FarmerPost> postOptional = farmerPostService.getPostById(postId);
            
            if (!postOptional.isPresent()) {
                logger.error("Post not found with ID: {}", postId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            
            FarmerPost post = postOptional.get();
            
            // Check if the index is valid
            if (post.getImages() == null || post.getImages().isEmpty()) {
                logger.error("No images found for post: {}", postId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            
            if (index < 0 || index >= post.getImages().size()) {
                logger.error("Invalid image index: {} for post: {} with {} images", 
                        index, postId, post.getImages().size());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            
            PostImage image = post.getImages().get(index);
            logger.info("Found image: {} of type: {}", image.getFileName(), image.getContentType());
            
            HttpHeaders headers = new HttpHeaders();
            
            // Set content type based on the stored content type
            headers.setContentType(MediaType.parseMediaType(image.getContentType()));
            
            // Set filename for download
            headers.setContentDispositionFormData("inline", image.getFileName());
            
            return new ResponseEntity<>(image.getData(), headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving image for post ID: {} at index: {}", postId, index, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

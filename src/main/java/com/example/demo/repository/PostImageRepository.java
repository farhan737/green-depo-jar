package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.PostImage;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {
    
    // Find all images for a specific post
    List<PostImage> findByPostId(Long postId);
    
    // Delete all images for a specific post
    void deleteByPostId(Long postId);
}

package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.PostReaction;

@Repository
public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {
    
    // Find a reaction by post id and user id
    Optional<PostReaction> findByPostIdAndUserIdAndUserType(Long postId, Long userId, String userType);
    
    // Count likes for a post
    long countByPostIdAndPositive(Long postId, boolean positive);
    
    // Delete all reactions for a post
    void deleteByPostId(Long postId);
}

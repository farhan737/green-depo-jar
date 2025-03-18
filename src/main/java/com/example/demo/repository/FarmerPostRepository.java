package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.model.FarmerPost;

@Repository
public interface FarmerPostRepository extends JpaRepository<FarmerPost, Long> {
    
    // Find all posts by a specific farmer
    List<FarmerPost> findByFarmerIdOrderByCreatedAtDesc(Long farmerId);
    
    // Find all posts ordered by creation date (newest first)
    List<FarmerPost> findAllByOrderByCreatedAtDesc();
    
    // Find all posts ordered by total reactions (most reactions first)
    @Query("SELECT p FROM FarmerPost p ORDER BY (p.likeCount + p.dislikeCount) DESC")
    List<FarmerPost> findAllOrderByTotalReactionsDesc();
    
    // Find all posts ordered by likes (most likes first)
    List<FarmerPost> findAllByOrderByLikeCountDesc();
    
    // Find all posts ordered by dislikes (most dislikes first)
    List<FarmerPost> findAllByOrderByDislikeCountDesc();
}

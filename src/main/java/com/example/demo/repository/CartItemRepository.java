package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.CartItem;
import com.example.demo.model.Consumer;
import com.example.demo.model.Product;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByConsumerId(Long consumerId);
    Optional<CartItem> findByConsumerAndProduct(Consumer consumer, Product product);
    void deleteByConsumerAndProduct(Consumer consumer, Product product);
}

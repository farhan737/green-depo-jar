package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Consumer;
import com.example.demo.model.Farmer;
import com.example.demo.model.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByConsumer(Consumer consumer);
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByOrderItemsProductFarmer(Farmer farmer);
}

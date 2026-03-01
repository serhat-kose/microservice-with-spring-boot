package com.example.orderservice.repository;

import com.example.orderservice.model.Order;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(String userId);
}

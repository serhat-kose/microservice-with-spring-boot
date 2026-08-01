package com.serhat.ecommerce.orderservice.repository;

import com.serhat.ecommerce.orderservice.model.Order;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(String userId);
}

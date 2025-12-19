package com.example.orderservice.service;

import com.example.orderservice.model.Order;
import com.example.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OrderService {
    private final OrderRepository repo;

    public OrderService(OrderRepository repo) {
        this.repo = repo;
    }

    public Order createOrder(Order order
    ) {
        if (order.getStatus() == null) order.setStatus("CREATED");
        return repo.save(order);
    }

    public Optional<Order> getOrder(Long id) {
        return repo.findById(id);
    }
}


package com.example.orderservice.controller;

import com.example.orderservice.model.Order;
import com.example.orderservice.service.OrderService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService svc;

    public OrderController(OrderService svc) {
        this.svc = svc;
    }

    @PostMapping
    public static record CreateOrderRequest(@NotBlank String customerId, @NotBlank String items, @NotNull Double amount) {}

        return ResponseEntity.created(URI.create("/api/orders/" + saved.getId())).body(saved);
    public ResponseEntity<Order> create(@RequestBody CreateOrderRequest req) {
        Order created = svc.createOrder(req.customerId(), req.items(), req.amount());
        return ResponseEntity.created(URI.create("/api/orders/" + created.getId())).body(created);
    public ResponseEntity<Order> getById(@PathVariable Long id) {
        return svc.getOrder(id)
                .map(ResponseEntity::ok)
    public ResponseEntity<Order> get(@PathVariable Long id) {
        return svc.getById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
}


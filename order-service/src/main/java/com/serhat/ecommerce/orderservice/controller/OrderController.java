package com.serhat.ecommerce.orderservice.controller;

import com.serhat.ecommerce.orderservice.dto.OrderDtos;
import com.serhat.ecommerce.orderservice.model.Order;
import com.serhat.ecommerce.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getByUser(@PathVariable String userId) {
        return ResponseEntity.ok(orderService.getByUser(userId));
    }

    // manuel oluşturma (isteğe bağlı) -> doğrudan order-create event produce etmek yerine orchestrator akışına uygun event yayınla
    @PostMapping
    public ResponseEntity<Void> createOrder(@RequestBody OrderDtos.CreateOrderRequest req) {
        kafkaTemplate.send("order-create", req);
        return ResponseEntity.accepted().build();
    }
}

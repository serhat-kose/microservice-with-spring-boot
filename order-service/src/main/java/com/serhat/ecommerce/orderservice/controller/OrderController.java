package com.serhat.ecommerce.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhat.ecommerce.orderservice.dto.OrderDtos;
import com.serhat.ecommerce.orderservice.dto.OrderDtos.OrderResponse;
import com.serhat.ecommerce.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(OrderResponse.from(orderService.getById(id)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getByUser(@PathVariable String userId) {
        return ResponseEntity.ok(orderService.getByUser(userId).stream().map(OrderResponse::from).toList());
    }

    // Publishes an order-create event for order-service's own OrderCreateListener to
    // pick up (a single write path, consistent with how the saga-orchestrator drives
    // order creation from a cart checkout).
    @PostMapping
    public ResponseEntity<Void> createOrder(@Valid @RequestBody OrderDtos.CreateOrderRequest req) throws Exception {
        kafkaTemplate.send("order-create", objectMapper.writeValueAsString(req));
        return ResponseEntity.accepted().build();
    }
}

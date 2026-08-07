package com.serhat.ecommerce.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhat.ecommerce.commons.security.CurrentUser;
import com.serhat.ecommerce.orderservice.dto.OrderDtos;
import com.serhat.ecommerce.orderservice.dto.OrderDtos.OrderResponse;
import com.serhat.ecommerce.orderservice.model.Order;
import com.serhat.ecommerce.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Order history is scoped to the authenticated caller. Previously {@code /user/{userId}}
 * took the owner from the URL, so any logged-in account could read anyone's order history,
 * and {@code POST} took the userId from the request body, so an order could be placed on
 * another customer's behalf.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        Order order = orderService.getById(id);
        requireOwnerOrAdmin(order);
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    /** The caller's own order history. */
    @GetMapping("/my")
    public ResponseEntity<List<OrderResponse>> myOrders() {
        return ResponseEntity.ok(orderService.getByUser(CurrentUser.requireUserId()).stream()
                .map(OrderResponse::from)
                .toList());
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> getByUser(@PathVariable String userId) {
        return ResponseEntity.ok(orderService.getByUser(userId).stream().map(OrderResponse::from).toList());
    }

    /**
     * Publishes an order-create event for this service's own listener to pick up, keeping a
     * single write path consistent with how the saga drives order creation from a checkout.
     */
    @PostMapping
    public ResponseEntity<Void> createOrder(@Valid @RequestBody OrderDtos.CreateOrderRequest req) throws Exception {
        // The owner is taken from the verified identity, never from the request body.
        req.setUserId(CurrentUser.requireUserId());
        kafkaTemplate.send("order-create", objectMapper.writeValueAsString(req));
        return ResponseEntity.accepted().build();
    }

    private void requireOwnerOrAdmin(Order order) {
        var user = CurrentUser.get().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated"));
        if (user.hasRole("ADMIN") || user.userId().equals(order.getUserId())) {
            return;
        }
        // Deliberately 404 rather than 403 so the response does not confirm that an order
        // with this id exists for someone else.
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + order.getId());
    }
}

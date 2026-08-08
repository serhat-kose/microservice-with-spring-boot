package com.serhat.ecommerce.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhat.ecommerce.commons.event.EventEnvelope;
import com.serhat.ecommerce.commons.security.CurrentUser;
import com.serhat.ecommerce.orderservice.dto.OrderDtos;
import com.serhat.ecommerce.orderservice.dto.OrderDtos.OrderDetail;
import com.serhat.ecommerce.orderservice.dto.OrderDtos.OrderSummary;
import com.serhat.ecommerce.orderservice.dto.OrderDtos.PageResponse;
import com.serhat.ecommerce.orderservice.model.Order;
import com.serhat.ecommerce.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Order history is scoped to the authenticated caller: the owner comes from the verified
 * identity, never from the URL or the request body.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final int MAX_PAGE_SIZE = 50;

    private final OrderService orderService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /** Full detail including the lines, which no endpoint previously returned. */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDetail> getOrder(@PathVariable Long id) {
        Order order = orderService.getById(id);
        requireOwnerOrAdmin(order);
        return ResponseEntity.ok(OrderDetail.from(order));
    }

    @GetMapping("/my")
    public ResponseEntity<PageResponse<OrderSummary>> myOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(
                orderService.getByUser(CurrentUser.requireUserId(), pageable(page, size))
                        .map(OrderSummary::from)));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<OrderSummary>> getByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(
                orderService.getByUser(userId, pageable(page, size)).map(OrderSummary::from)));
    }

    /**
     * Publishes an order-create event for this service's own listener, keeping a single
     * write path consistent with how the saga drives order creation from a cart checkout.
     */
    @PostMapping
    public ResponseEntity<Void> createOrder(@Valid @RequestBody OrderDtos.CreateOrderRequest req)
            throws Exception {
        req.setUserId(CurrentUser.requireUserId());
        EventEnvelope<Object> envelope = EventEnvelope.of("order.create", null, req);
        kafkaTemplate.send("order-create", req.getUserId(), objectMapper.writeValueAsString(envelope));
        return ResponseEntity.accepted().build();
    }

    private PageRequest pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private void requireOwnerOrAdmin(Order order) {
        var user = CurrentUser.get().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated"));
        if (user.hasRole("ADMIN") || user.userId().equals(order.getUserId())) {
            return;
        }
        // 404 rather than 403 so the response does not confirm that this order exists.
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + order.getId());
    }
}

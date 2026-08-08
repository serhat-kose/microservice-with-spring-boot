package com.serhat.ecommerce.orderservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhat.ecommerce.commons.event.EventEnvelope;
import com.serhat.ecommerce.orderservice.exception.OrderNotFoundException;
import com.serhat.ecommerce.orderservice.model.Order;
import com.serhat.ecommerce.orderservice.model.OrderItem;
import com.serhat.ecommerce.orderservice.model.OrderStatus;
import com.serhat.ecommerce.orderservice.model.ShippingAddress;
import com.serhat.ecommerce.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Creates the order from a checkout payload and announces it so the saga can continue.
     *
     * <p>Lines are persisted as rows rather than a JSON blob, and the address is snapshotted
     * onto the order so the shipment step has a real destination - it previously received
     * the literal string "default".
     */
    @Transactional
    public Order createOrderFromEvent(JsonNode payload) {
        String userId = text(payload, "userId");
        if (userId == null) {
            throw new IllegalArgumentException("Checkout payload carries no userId");
        }

        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.CREATED)
                .couponCode(text(payload, "couponCode"))
                .discountAmount(decimal(payload, "discountAmount"))
                .shippingCost(decimal(payload, "shippingCost"))
                .taxAmount(decimal(payload, "taxAmount"))
                .shippingAddress(address(payload.get("shippingAddress")))
                .build();

        JsonNode items = payload.get("items");
        if (items == null || !items.isArray() || items.isEmpty()) {
            throw new IllegalArgumentException("Checkout payload carries no items");
        }
        for (JsonNode item : items) {
            order.addItem(OrderItem.builder()
                    .productId(item.path("productId").asLong())
                    .sku(text(item, "sku"))
                    .productName(text(item, "productName") == null
                            ? "Product " + item.path("productId").asLong() : text(item, "productName"))
                    .quantity(item.path("quantity").asInt(1))
                    .unitPrice(decimal(item, "price") != null ? decimal(item, "price") : decimal(item, "unitPrice"))
                    .build());
        }

        order.recalculateTotals();
        Order saved = orderRepository.save(order);

        publish("order-created", saved.getId().toString(), orderCreatedPayload(saved));
        return saved;
    }

    @Transactional(readOnly = true)
    public Order getById(Long id) {
        return orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<Order> getByUser(String userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable);
    }

    /**
     * Applies a status change only if the transition is legal.
     *
     * <p>Saga events are not ordered end to end, so a late {@code stock-reserved} could
     * previously drag a COMPLETED order back to RESERVED. Illegal moves are now logged and
     * ignored, and re-entering the current status is treated as a no-op so a redelivered
     * event is harmless.
     */
    @Transactional
    public void transitionTo(Long orderId, OrderStatus target, String failureReason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() == target) {
            return;
        }
        if (!order.getStatus().canTransitionTo(target)) {
            log.warn("Ignoring illegal transition for order {}: {} -> {}",
                    orderId, order.getStatus(), target);
            return;
        }

        order.setStatus(target);
        if (failureReason != null) {
            order.setFailureReason(failureReason);
        }
        orderRepository.save(order);
        log.info("Order {} moved to {}", orderId, target);
    }

    public void markReserved(String orderId) {
        transitionTo(parseId(orderId), OrderStatus.RESERVED, null);
    }

    public void markPaid(String orderId) {
        transitionTo(parseId(orderId), OrderStatus.PAID, null);
    }

    public void markCompleted(String orderId) {
        transitionTo(parseId(orderId), OrderStatus.COMPLETED, null);
    }

    public void markFailed(String orderId, String reason) {
        transitionTo(parseId(orderId), OrderStatus.FAILED, reason);
    }

    /**
     * Payload for {@code order-created}. Carries the buyer and the lines so downstream steps
     * (stock, notification, review eligibility) have what they need without calling back.
     */
    private Map<String, Object> orderCreatedPayload(Order order) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("productId", item.getProductId());
            line.put("sku", item.getSku());
            line.put("productName", item.getProductName());
            line.put("quantity", item.getQuantity());
            line.put("price", item.getUnitPrice());
            items.add(line);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", String.valueOf(order.getId()));
        payload.put("userId", order.getUserId());
        payload.put("items", items);
        payload.put("amount", order.getTotalAmount());
        payload.put("couponCode", order.getCouponCode());
        payload.put("status", order.getStatus().name());
        if (order.getShippingAddress() != null) {
            payload.put("address", order.getShippingAddress().singleLine());
        }
        return payload;
    }

    private void publish(String topic, String key, Object payload) {
        try {
            EventEnvelope<Object> envelope = EventEnvelope.of("order.created", null, payload);
            kafkaTemplate.send(topic, key, objectMapper.writeValueAsString(envelope));
        } catch (Exception e) {
            // Rethrown so the surrounding transaction rolls back rather than leaving an
            // order that the saga will never hear about.
            throw new IllegalStateException("Failed to publish " + topic + " for order " + key, e);
        }
    }

    private Long parseId(String orderId) {
        try {
            return Long.valueOf(orderId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Malformed orderId in event: " + orderId, e);
        }
    }

    private ShippingAddress address(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return ShippingAddress.builder()
                .recipientName(text(node, "recipientName"))
                .phone(text(node, "phone"))
                .line1(text(node, "line1"))
                .line2(text(node, "line2"))
                .city(text(node, "city"))
                .district(text(node, "district"))
                .postalCode(text(node, "postalCode"))
                .country(text(node, "country"))
                .build();
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.asText("0"));
    }
}

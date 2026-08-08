package com.serhat.ecommerce.orderservice.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhat.ecommerce.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Advances the order's status as the saga progresses. Ordering between these topics is not
 * guaranteed, so the status change itself is validated against the transition table in
 * {@code OrderService} rather than applied blindly.
 */
@Component
@RequiredArgsConstructor
public class OrderEventsListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-completed", groupId = "order-group")
    public void onOrderCompleted(String message) throws Exception {
        JsonNode payload = payloadOf(message);
        orderService.markCompleted(required(payload, "orderId"));
    }

    @KafkaListener(topics = "order-failed", groupId = "order-group")
    public void onOrderFailed(String message) throws Exception {
        JsonNode payload = payloadOf(message);
        orderService.markFailed(required(payload, "orderId"), text(payload, "reason"));
    }

    @KafkaListener(topics = "payment-result", groupId = "order-group")
    public void onPaymentResult(String message) throws Exception {
        JsonNode payload = payloadOf(message);
        if ("SUCCESS".equalsIgnoreCase(text(payload, "status"))) {
            orderService.markPaid(required(payload, "orderId"));
        }
    }

    @KafkaListener(topics = "stock-reserved", groupId = "order-group")
    public void onStockReserved(String message) throws Exception {
        JsonNode payload = payloadOf(message);
        orderService.markReserved(required(payload, "orderId"));
    }

    private JsonNode payloadOf(String message) throws Exception {
        JsonNode root = objectMapper.readTree(message);
        JsonNode payload = root.get("payload");
        return payload == null || payload.isNull() ? root : payload;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String required(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) {
            // Thrown so the shared error handler retries and then dead-letters, rather than
            // the event being silently dropped.
            throw new IllegalArgumentException("Event is missing " + field);
        }
        return value;
    }
}

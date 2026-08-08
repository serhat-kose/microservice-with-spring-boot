package com.serhat.ecommerce.reviewservice.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhat.ecommerce.reviewservice.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Builds the verified-purchase record from completed orders, which is what gates who may
 * review a product.
 *
 * <p>Recording is idempotent (a unique constraint on user+product, plus an existence check),
 * so a redelivered order-completed event is harmless.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompletedListener {

    private final ReviewService reviewService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-completed", groupId = "review-group")
    public void onOrderCompleted(String message) throws Exception {
        JsonNode payload = payloadOf(objectMapper.readTree(message));

        String userId = text(payload, "userId");
        String orderId = text(payload, "orderId");
        if (userId == null || orderId == null) {
            // Until order-completed carries the buyer, there is nothing to attribute the
            // purchase to; logged rather than retried because a replay would not add it.
            log.warn("order-completed without userId/orderId, cannot record purchase: {}", message);
            return;
        }

        JsonNode items = payload.get("items");
        if (items == null || !items.isArray()) {
            log.warn("order-completed for order {} carries no items", orderId);
            return;
        }

        for (JsonNode item : items) {
            JsonNode productId = item.get("productId");
            if (productId != null && !productId.isNull()) {
                reviewService.recordPurchase(userId, productId.asLong(), orderId);
            }
        }
    }

    private JsonNode payloadOf(JsonNode root) {
        JsonNode payload = root.get("payload");
        return payload == null || payload.isNull() ? root : payload;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}

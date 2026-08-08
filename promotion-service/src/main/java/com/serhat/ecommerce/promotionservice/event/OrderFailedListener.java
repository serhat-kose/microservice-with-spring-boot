package com.serhat.ecommerce.promotionservice.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhat.ecommerce.promotionservice.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Compensating action for the coupon step of the checkout saga: when an order fails after a
 * coupon was consumed, the redemption is given back so the customer does not lose a use for
 * an order that never completed.
 *
 * <p>Idempotent - once the redemption row is gone, a redelivered order-failed event finds
 * nothing to release and does nothing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderFailedListener {

    private final CouponService couponService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-failed", groupId = "promotion-group")
    public void onOrderFailed(String message) throws Exception {
        JsonNode payload = payloadOf(objectMapper.readTree(message));
        JsonNode orderId = payload.get("orderId");
        if (orderId == null || orderId.isNull()) {
            log.warn("order-failed without orderId, cannot release a coupon: {}", message);
            return;
        }
        couponService.releaseForOrder(orderId.asText());
    }

    private JsonNode payloadOf(JsonNode root) {
        JsonNode payload = root.get("payload");
        return payload == null || payload.isNull() ? root : payload;
    }
}

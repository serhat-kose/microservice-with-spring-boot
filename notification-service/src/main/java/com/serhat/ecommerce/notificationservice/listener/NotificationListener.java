package com.serhat.ecommerce.notificationservice.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhat.ecommerce.notificationservice.dto.NotificationPayload;
import com.serhat.ecommerce.notificationservice.dto.OrderItem;
import com.serhat.ecommerce.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Turns order-lifecycle events into customer emails.
 *
 * <p>Deliberately only reacts to the moments a customer cares about: the order completing,
 * the order failing, and the parcel being booked. It previously also mailed on
 * {@code stock-reserved} and {@code payment-result}, so a single successful checkout sent
 * three emails within seconds for internal saga steps the customer has no use for.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationService notificationService;
    private final ObjectMapper mapper;

    @KafkaListener(topics = "order-completed", groupId = "notification-group")
    public void onOrderCompleted(String message) throws Exception {
        JsonNode payload = payloadOf(message);
        NotificationPayload notification = base(payload, "ORDER_COMPLETED");
        notification.setItems(items(payload));
        notificationService.sendOrderNotification(notification);
    }

    @KafkaListener(topics = "order-failed", groupId = "notification-group")
    public void onOrderFailed(String message) throws Exception {
        JsonNode payload = payloadOf(message);
        NotificationPayload notification = base(payload, "ORDER_FAILED");
        notification.setReason(text(payload, "reason"));
        notificationService.sendOrderNotification(notification);
    }

    /** Tells the customer their parcel is on its way, with the number to track it. */
    @KafkaListener(topics = "shipment-result", groupId = "notification-group")
    public void onShipmentResult(String message) throws Exception {
        JsonNode payload = payloadOf(message);
        if (!"SCHEDULED".equalsIgnoreCase(text(payload, "status"))) {
            // A failed shipment is announced by order-failed instead, so the customer is
            // not told twice about the same problem.
            return;
        }
        NotificationPayload notification = base(payload, "SHIPMENT_SCHEDULED");
        notification.setTrackingNumber(text(payload, "trackingNumber"));
        notification.setCarrier(text(payload, "carrier"));
        notificationService.sendOrderNotification(notification);
    }

    private NotificationPayload base(JsonNode payload, String eventType) {
        NotificationPayload notification = new NotificationPayload();
        notification.setEventType(eventType);
        notification.setOrderId(text(payload, "orderId"));
        notification.setUserId(text(payload, "userId"));
        notification.setEmail(text(payload, "email"));
        return notification;
    }

    private List<OrderItem> items(JsonNode payload) {
        JsonNode items = payload.get("items");
        if (items == null || !items.isArray()) {
            return List.of();
        }
        return mapper.convertValue(items,
                mapper.getTypeFactory().constructCollectionType(List.class, OrderItem.class));
    }

    private JsonNode payloadOf(String message) throws Exception {
        JsonNode root = mapper.readTree(message);
        JsonNode payload = root.get("payload");
        return payload == null || payload.isNull() ? root : payload;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}

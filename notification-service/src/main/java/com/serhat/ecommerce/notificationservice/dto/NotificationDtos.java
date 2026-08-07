package com.serhat.ecommerce.notificationservice.dto;

import com.serhat.ecommerce.notificationservice.model.Notification;

import java.time.Instant;

public class NotificationDtos {

    public record NotificationResponse(
            Long id,
            String eventType,
            String orderId,
            String status,
            Instant createdAt
    ) {
        public static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.getId(), n.getEventType(), n.getOrderId(),
                    n.getStatus(), n.getCreatedAt());
        }
    }
}

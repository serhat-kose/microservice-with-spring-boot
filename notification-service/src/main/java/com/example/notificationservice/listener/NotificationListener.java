package com.example.notificationservice.listener;

import com.example.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NotificationListener {
    private final NotificationService notificationService;
    private final ObjectMapper mapper = new ObjectMapper();

    public NotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "order-completed", groupId = "notification-group")
    public void onOrderCompleted(String message) throws Exception {
        Map<String, Object> m = mapper.readValue(message, Map.class);
        String orderId = (String) m.get("orderId");
        notificationService.sendNotification("Order " + orderId + " completed.");
    }

    @KafkaListener(topics = "order-failed", groupId = "notification-group")
    public void onOrderFailed(String message) throws Exception {
        Map<String, Object> m = mapper.readValue(message, Map.class);
        String orderId = (String) m.get("orderId");
        String reason = (String) m.getOrDefault("reason", "unknown");
        notificationService.sendNotification("Order " + orderId + " failed: " + reason);
    }
}

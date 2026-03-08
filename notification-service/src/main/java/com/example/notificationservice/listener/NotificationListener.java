package com.example.notificationservice.listener;

import com.example.notificationservice.dto.NotificationPayload;
import com.example.notificationservice.dto.OrderItem;
import com.example.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
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
        String orderId = String.valueOf(m.get("orderId"));

        NotificationPayload payload = new NotificationPayload();
        payload.setEventType("ORDER_COMPLETED");
        payload.setOrderId(orderId);
        payload.setUserId((String) m.get("userId"));
        // try to extract items if present
        Object itemsObj = m.get("items");
        if (itemsObj != null) {
            List<OrderItem> items = mapper.convertValue(itemsObj, mapper.getTypeFactory().constructCollectionType(List.class, OrderItem.class));
            payload.setItems(items);
        }
        notificationService.sendOrderNotification(payload);
    }

    @KafkaListener(topics = "order-failed", groupId = "notification-group")
    public void onOrderFailed(String message) throws Exception {
        Map<String, Object> m = mapper.readValue(message, Map.class);
        String orderId = String.valueOf(m.get("orderId"));

        NotificationPayload payload = new NotificationPayload();
        payload.setEventType("ORDER_FAILED");
        payload.setOrderId(orderId);
        payload.setUserId((String) m.get("userId"));
        payload.setEmail((String) m.get("email"));
        notificationService.sendOrderNotification(payload);
    }

    // Yeni: stok rezervasyonu oluştuğunda kullanıcıya bilgilendirme
    @KafkaListener(topics = "stock-reserved", groupId = "notification-group")
    public void onStockReserved(String message) throws Exception {
        Map<String, Object> m = mapper.readValue(message, Map.class);
        String orderId = String.valueOf(m.get("orderId"));

        NotificationPayload payload = new NotificationPayload();
        payload.setEventType("RESERVATION_CREATED");
        payload.setOrderId(orderId);
        payload.setUserId((String) m.get("userId"));
        Object itemsObj = m.get("items");
        if (itemsObj != null) {
            List<OrderItem> items = mapper.convertValue(itemsObj, mapper.getTypeFactory().constructCollectionType(List.class, OrderItem.class));
            payload.setItems(items);
        }
        notificationService.sendOrderNotification(payload);
    }

    // Yeni: ödeme sonucu (payment-result) -> başarılıysa mail gönder
    @KafkaListener(topics = "payment-result", groupId = "notification-group")
    public void onPaymentResult(String message) throws Exception {
        Map<String, Object> m = mapper.readValue(message, Map.class);
        String status = (String) m.get("status");
        String orderId = String.valueOf(m.get("orderId"));
        if ("SUCCESS".equalsIgnoreCase(status)) {
            NotificationPayload payload = new NotificationPayload();
            payload.setEventType("PAYMENT_COMPLETED");
            payload.setOrderId(orderId);
            payload.setUserId((String) m.get("userId"));
            payload.setEmail((String) m.get("email"));
            Object itemsObj = m.get("items");
            if (itemsObj != null) {
                List<OrderItem> items = mapper.convertValue(itemsObj, mapper.getTypeFactory().constructCollectionType(List.class, OrderItem.class));
                payload.setItems(items);
            }
            notificationService.sendOrderNotification(payload);
        }
    }
}

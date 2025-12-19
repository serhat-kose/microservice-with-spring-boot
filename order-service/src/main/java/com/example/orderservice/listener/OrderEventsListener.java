package com.example.orderservice.listener;

import com.example.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OrderEventsListener {
    private final OrderService orderService;
    private final ObjectMapper mapper = new ObjectMapper();

    public OrderEventsListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(topics = "order-completed", groupId = "order-group")
    public void onOrderCompleted(String message) throws Exception {
        Map<?,?> m = mapper.readValue(message, Map.class);
        String orderId = (String) m.get("orderId");
        orderService.markCompleted(orderId);
    }

    @KafkaListener(topics = "order-failed", groupId = "order-group")
    public void onOrderFailed(String message) throws Exception {
        Map<?,?> m = mapper.readValue(message, Map.class);
        String orderId = (String) m.get("orderId");
        orderService.markFailed(orderId, (String) m.get("reason"));
    }

    @KafkaListener(topics = "payment-result", groupId = "order-group")
    public void onPaymentResult(String message) throws Exception {
        Map<?,?> m = mapper.readValue(message, Map.class);
        String orderId = (String) m.get("orderId");
        String status = (String) m.get("status");
        if ("SUCCESS".equalsIgnoreCase(status)) {
            orderService.markPaid(orderId);
        }
    }

    @KafkaListener(topics = "stock-reserved", groupId = "order-group")
    public void onStockReserved(String message) throws Exception {
        Map<?,?> m = mapper.readValue(message, Map.class);
        String orderId = (String) m.get("orderId");
        orderService.markReserved(orderId);
    }
}


package com.serhat.ecommerce.orderservice.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhat.ecommerce.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCreateListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-create", groupId = "order-service")
    public void onOrderCreate(String message) throws Exception {
        JsonNode root = objectMapper.readTree(message);
        JsonNode payload = root.get("payload");
        orderService.createOrderFromEvent(payload == null || payload.isNull() ? root : payload);
    }
}

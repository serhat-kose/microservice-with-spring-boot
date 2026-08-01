package com.serhat.ecommerce.orderservice.listener;

import com.serhat.ecommerce.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
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
        Map<String, Object> map = objectMapper.readValue(message, Map.class);
        orderService.createOrderFromEvent(map);
    }
}


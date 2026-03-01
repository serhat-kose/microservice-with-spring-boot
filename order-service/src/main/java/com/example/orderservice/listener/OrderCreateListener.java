package com.example.orderservice.listener;

import com.example.orderservice.service.OrderService;
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
    public void onOrderCreate(Object payload) {
        // payload muhtemelen JSON; spring-kafka ile object olarak gelirse map'e çevir
        Map<String, Object> map = objectMapper.convertValue(payload, Map.class);
        orderService.createOrderFromEvent(map);
    }
}


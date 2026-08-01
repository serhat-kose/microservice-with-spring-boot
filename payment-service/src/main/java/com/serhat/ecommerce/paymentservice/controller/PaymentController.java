package com.serhat.ecommerce.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper = new ObjectMapper();

    public PaymentController(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    @PostMapping("/process")
    public ResponseEntity<Map<String,String>> process(@RequestBody Map<String, Object> req) throws Exception {
        // Basit simülasyon: ödeme hemen başarılı (gerçekte ödeme gateway entegrasyonu olacak)
        String orderId = (String) req.get("orderId");
        String status = "SUCCESS";
        String event = mapper.writeValueAsString(Map.of(
                "orderId", orderId,
                "paymentId", UUID.randomUUID().toString(),
                "status", status
        ));
        kafka.send("payment-result", orderId, event);
        return ResponseEntity.ok(Map.of("status", status));
    }
}


package com.example.sagaorchestrator.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class Orchestrator {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public Orchestrator(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "cart-checkout", groupId = "saga-orchestrator")
    public void onCartCheckout(Object payload) throws Exception {
        Map<String, Object> event = objectMapper.convertValue(payload, Map.class);
        kafkaTemplate.send("order-create", objectMapper.writeValueAsString(event));
    }

    // 1) order-created -> try reserve stock
    @KafkaListener(topics = "order-created", groupId = "saga-orchestrator")
    public void onOrderCreated(String message) throws Exception {
        Map<String, Object> m = objectMapper.readValue(message, Map.class);
        String orderId = (String) m.get("orderId");
        String reserveReq = objectMapper.writeValueAsString(Map.of("orderId", orderId, "items", m.get("items")));
        kafkaTemplate.send("stock-reserve-request", orderId, reserveReq);
    }

    // 2) if stock reserved -> request payment
    @KafkaListener(topics = "stock-reserved", groupId = "saga-orchestrator")
    public void onStockReserved(String message) throws Exception {
        Map<String, Object> m = objectMapper.readValue(message, Map.class);
        String orderId = (String) m.get("orderId");
        String paymentReq = objectMapper.writeValueAsString(Map.of("orderId", orderId, "amount", m.get("amount")));
        kafkaTemplate.send("payment-request", orderId, paymentReq);
    }

    // 3) if stock reservation failed -> order failed
    @KafkaListener(topics = "stock-reservation-failed", groupId = "saga-orchestrator")
    public void onStockReservationFailed(String message) throws Exception {
        Map<String, Object> m = objectMapper.readValue(message, Map.class);
        String orderId = (String) m.get("orderId");
        String evt = objectMapper.writeValueAsString(Map.of("orderId", orderId, "status", "FAILED", "reason", "stock_insufficient"));
        kafkaTemplate.send("order-failed", orderId, evt);
    }

    // 4) payment result
    @KafkaListener(topics = "payment-result", groupId = "saga-orchestrator")
    public void onPaymentResult(String message) throws Exception {
        Map<String, Object> m = objectMapper.readValue(message, Map.class);
        String orderId = (String) m.get("orderId");
        String status = (String) m.get("status");
        if ("SUCCESS".equalsIgnoreCase(status)) {
            String shipmentReq = objectMapper.writeValueAsString(Map.of("orderId", orderId, "address", m.getOrDefault("address","default")));
            kafkaTemplate.send("shipment-request", orderId, shipmentReq);
        } else {
            String release = objectMapper.writeValueAsString(Map.of("orderId", orderId, "items", m.get("items")));
            kafkaTemplate.send("stock-release", orderId, release);
            String evt = objectMapper.writeValueAsString(Map.of("orderId", orderId, "status", "FAILED", "reason", "payment_failed"));
            kafkaTemplate.send("order-failed", orderId, evt);
        }
    }

    // 5) shipment result
    @KafkaListener(topics = "shipment-result", groupId = "saga-orchestrator")
    public void onShipmentResult(String message) throws Exception {
        Map<String, Object> m = objectMapper.readValue(message, Map.class);
        String orderId = (String) m.get("orderId");
        String status = (String) m.get("status");
        if ("SCHEDULED".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) {
            String evt = objectMapper.writeValueAsString(Map.of("orderId", orderId, "status", "COMPLETED"));
            kafkaTemplate.send("order-completed", orderId, evt);
        } else {
            String refund = objectMapper.writeValueAsString(Map.of("orderId", orderId, "amount", m.get("amount")));
            kafkaTemplate.send("payment-refund", orderId, refund);
            String evt = objectMapper.writeValueAsString(Map.of("orderId", orderId, "status", "FAILED", "reason", "shipment_failed"));
            kafkaTemplate.send("order-failed", orderId, evt);
        }
    }
}

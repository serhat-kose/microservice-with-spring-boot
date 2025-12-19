package com.example.sagaorchestrator.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class Orchestrator {
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper = new ObjectMapper();

    public Orchestrator(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    // 1) order-created -> try reserve stock
    @KafkaListener(topics = "order-created", groupId = "saga-orchestrator")
    public void onOrderCreated(String message) throws Exception {
        Map<?,?> m = mapper.readValue(message, Map.class);
        String orderId = (String) m.get("orderId");
        // forward reserve request (include items)
        String reserveReq = mapper.writeValueAsString(Map.of("orderId", orderId, "items", m.get("items")));
        kafka.send("stock-reserve-request", orderId, reserveReq);
    }

    // 2) if stock reserved -> request payment
    @KafkaListener(topics = "stock-reserved", groupId = "saga-orchestrator")
    public void onStockReserved(String message) throws Exception {
        Map<?,?> m = mapper.readValue(message, Map.class);
        String orderId = (String) m.get("orderId");
        // ask payment (amount should be retrieved/propagated by order-created; for simplicity orchestrator expects amount in previous order-created event)
        // In this demo we assume payment-request contains orderId and amount; real impl: store saga state to get amount
        // Here we ask payment-service with minimal info; payment-service simulates success
        String paymentReq = mapper.writeValueAsString(Map.of("orderId", orderId, "amount", m.get("amount")));
        kafka.send("payment-request", orderId, paymentReq);
    }

    // 3) if stock reservation failed -> order failed
    @KafkaListener(topics = "stock-reservation-failed", groupId = "saga-orchestrator")
    public void onStockReservationFailed(String message) throws Exception {
        Map<?,?> m = mapper.readValue(message, Map.class);
        String orderId = (String) m.get("orderId");
        String evt = mapper.writeValueAsString(Map.of("orderId", orderId, "status", "FAILED", "reason", "stock_insufficient"));
        kafka.send("order-failed", orderId, evt);
    }

    // 4) payment result
    @KafkaListener(topics = "payment-result", groupId = "saga-orchestrator")
    public void onPaymentResult(String message) throws Exception {
        Map<?,?> m = mapper.readValue(message, Map.class);
        String orderId = (String) m.get("orderId");
        String status = (String) m.get("status");
        if ("SUCCESS".equalsIgnoreCase(status)) {
            String shipmentReq = mapper.writeValueAsString(Map.of("orderId", orderId, "address", m.getOrDefault("address","default")));
            kafka.send("shipment-request", orderId, shipmentReq);
        } else {
            // payment failed -> release stock and fail order
            String release = mapper.writeValueAsString(Map.of("orderId", orderId, "items", m.get("items")));
            kafka.send("stock-release", orderId, release);
            String evt = mapper.writeValueAsString(Map.of("orderId", orderId, "status", "FAILED", "reason", "payment_failed"));
            kafka.send("order-failed", orderId, evt);
        }
    }

    // 5) shipment result
    @KafkaListener(topics = "shipment-result", groupId = "saga-orchestrator")
    public void onShipmentResult(String message) throws Exception {
        Map<?,?> m = mapper.readValue(message, Map.class);
        String orderId = (String) m.get("orderId");
        String status = (String) m.get("status");
        if ("SCHEDULED".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) {
            String evt = mapper.writeValueAsString(Map.of("orderId", orderId, "status", "COMPLETED"));
            kafka.send("order-completed", orderId, evt);
        } else {
            // shipment failed -> refund payment and fail order
            String refund = mapper.writeValueAsString(Map.of("orderId", orderId, "amount", m.get("amount")));
            kafka.send("payment-refund", orderId, refund);
            String evt = mapper.writeValueAsString(Map.of("orderId", orderId, "status", "FAILED", "reason", "shipment_failed"));
            kafka.send("order-failed", orderId, evt);
        }
    }
}


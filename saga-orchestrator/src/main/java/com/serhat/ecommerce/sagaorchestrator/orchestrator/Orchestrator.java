package com.serhat.ecommerce.sagaorchestrator.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhat.ecommerce.sagaorchestrator.model.SagaState;
import com.serhat.ecommerce.sagaorchestrator.repository.SagaStateRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
public class Orchestrator {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final SagaStateRepository sagaStateRepository;

    public Orchestrator(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
                         SagaStateRepository sagaStateRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.sagaStateRepository = sagaStateRepository;
    }

    @KafkaListener(topics = "cart-checkout", groupId = "saga-orchestrator")
    public void onCartCheckout(String message) throws Exception {
        // forward as-is; order-service's createOrderFromEvent already reads either
        // "total" (cart-service's field name) or "amount" from the payload.
        kafkaTemplate.send("order-create", message);
    }

    // 1) order-created -> try reserve stock
    @KafkaListener(topics = "order-created", groupId = "saga-orchestrator")
    @Transactional
    public void onOrderCreated(String message) throws Exception {
        Map<String, Object> m = objectMapper.readValue(message, Map.class);
        String orderId = String.valueOf(m.get("orderId"));
        recordState(orderId, "STOCK_RESERVATION_REQUESTED");
        // include userId and amount so stock service and subsequent steps have context
        String reserveReq = objectMapper.writeValueAsString(Map.of(
                "orderId", orderId,
                "items", m.get("items"),
                "userId", m.get("userId"),
                "amount", m.get("amount")
        ));
        kafkaTemplate.send("stock-reserve-request", orderId, reserveReq);
    }

    // 2) if stock reserved -> request payment
    @KafkaListener(topics = "stock-reserved", groupId = "saga-orchestrator")
    @Transactional
    public void onStockReserved(String message) throws Exception {
        Map<String, Object> m = objectMapper.readValue(message, Map.class);
        String orderId = String.valueOf(m.get("orderId"));
        recordState(orderId, "PAYMENT_REQUESTED");
        String paymentReq = objectMapper.writeValueAsString(Map.of(
                "orderId", orderId,
                "amount", m.get("amount"),
                "userId", m.get("userId"),
                "items", m.get("items")
        ));
        kafkaTemplate.send("payment-request", orderId, paymentReq);
    }

    // 3) if stock reservation failed -> order failed
    @KafkaListener(topics = "stock-reservation-failed", groupId = "saga-orchestrator")
    @Transactional
    public void onStockReservationFailed(String message) throws Exception {
        Map<String, Object> m = objectMapper.readValue(message, Map.class);
        String orderId = String.valueOf(m.get("orderId"));
        recordState(orderId, "FAILED_INSUFFICIENT_STOCK");
        String evt = objectMapper.writeValueAsString(Map.of("orderId", orderId, "status", "FAILED", "reason", "stock_insufficient"));
        kafkaTemplate.send("order-failed", orderId, evt);
    }

    // 4) payment result
    @KafkaListener(topics = "payment-result", groupId = "saga-orchestrator")
    @Transactional
    public void onPaymentResult(String message) throws Exception {
        Map<String, Object> m = objectMapper.readValue(message, Map.class);
        String orderId = String.valueOf(m.get("orderId"));
        String status = (String) m.get("status");
        if ("SUCCESS".equalsIgnoreCase(status)) {
            recordState(orderId, "SHIPMENT_REQUESTED");
            String shipmentReq = objectMapper.writeValueAsString(Map.of("orderId", orderId, "address", m.getOrDefault("address","default")));
            kafkaTemplate.send("shipment-request", orderId, shipmentReq);
        } else {
            recordState(orderId, "FAILED_PAYMENT_COMPENSATING");
            String release = objectMapper.writeValueAsString(Map.of("orderId", orderId, "items", m.get("items")));
            kafkaTemplate.send("stock-release", orderId, release);
            String evt = objectMapper.writeValueAsString(Map.of("orderId", orderId, "status", "FAILED", "reason", "payment_failed"));
            kafkaTemplate.send("order-failed", orderId, evt);
        }
    }

    // 5) shipment result
    @KafkaListener(topics = "shipment-result", groupId = "saga-orchestrator")
    @Transactional
    public void onShipmentResult(String message) throws Exception {
        Map<String, Object> m = objectMapper.readValue(message, Map.class);
        String orderId = String.valueOf(m.get("orderId"));
        String status = (String) m.get("status");
        if ("SCHEDULED".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) {
            recordState(orderId, "COMPLETED");
            String evt = objectMapper.writeValueAsString(Map.of("orderId", orderId, "status", "COMPLETED"));
            kafkaTemplate.send("order-completed", orderId, evt);
        } else {
            recordState(orderId, "FAILED_SHIPMENT_COMPENSATING");
            String refund = objectMapper.writeValueAsString(Map.of("orderId", orderId, "amount", m.get("amount")));
            kafkaTemplate.send("payment-refund", orderId, refund);
            String evt = objectMapper.writeValueAsString(Map.of("orderId", orderId, "status", "FAILED", "reason", "shipment_failed"));
            kafkaTemplate.send("order-failed", orderId, evt);
        }
    }

    private void recordState(String orderId, String status) {
        SagaState state = sagaStateRepository.findById(orderId)
                .orElse(SagaState.builder().orderId(orderId).build());
        state.setStatus(status);
        sagaStateRepository.save(state);
    }
}

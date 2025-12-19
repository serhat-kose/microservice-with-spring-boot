package com.example.paymentservice.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class PaymentRefundListener {
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper = new ObjectMapper();

    public PaymentRefundListener(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    @KafkaListener(topics = "payment-refund", groupId = "payment-group")
    public void onRefundRequest(String message) throws Exception {
        Map<?,?> m = mapper.readValue(message, Map.class);
        String orderId = (String) m.get("orderId");
        // simulate refund
        String event = mapper.writeValueAsString(Map.of(
                "orderId", orderId,
                "refundId", UUID.randomUUID().toString(),
                "status", "REFUNDED"
        ));
        kafka.send("payment-refund-result", orderId, event);
    }
}


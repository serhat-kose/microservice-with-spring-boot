package com.serhat.ecommerce.paymentservice.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;

@Component
public class PaymentRequestListener {
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper = new ObjectMapper();

    public PaymentRequestListener(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    @KafkaListener(topics = "payment-request", groupId = "payment-group")
    public void onPaymentRequest(String message) throws Exception {
        Map<?,?> m = mapper.readValue(message, Map.class);
        String orderId = (String) m.get("orderId");
        // simulate processing -> success
        String event = mapper.writeValueAsString(Map.of(
                "orderId", orderId,
                "paymentId", UUID.randomUUID().toString(),
                "status", "SUCCESS",
                "userId", m.get("userId"),
                "items", m.get("items"),
                "amount", m.get("amount")
        ));
        kafka.send("payment-result", orderId, event);
    }
}

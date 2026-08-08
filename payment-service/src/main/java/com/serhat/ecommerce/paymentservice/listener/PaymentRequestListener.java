package com.serhat.ecommerce.paymentservice.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhat.ecommerce.commons.event.EventEnvelope;
import com.serhat.ecommerce.paymentservice.model.Payment;
import com.serhat.ecommerce.paymentservice.model.PaymentMethod;
import com.serhat.ecommerce.paymentservice.model.PaymentStatus;
import com.serhat.ecommerce.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRequestListener {

    private final PaymentService paymentService;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;

    @KafkaListener(topics = "payment-request", groupId = "payment-group")
    public void onPaymentRequest(String message) throws Exception {
        JsonNode payload = payloadOf(message);
        String orderId = required(payload, "orderId");

        Payment payment = paymentService.charge(
                orderId,
                text(payload, "userId"),
                amount(payload),
                method(text(payload, "paymentMethod")));

        // A decline is reported as a result, not thrown: it is a legitimate business
        // outcome that the saga must act on, not an infrastructure fault to retry.
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("orderId", orderId);
        event.put("userId", payment.getUserId());
        event.put("paymentId", payment.getProviderReference());
        event.put("status", payment.getStatus() == PaymentStatus.SUCCESS ? "SUCCESS" : "FAILED");
        event.put("amount", payment.getAmount());
        event.put("reason", payment.getFailureReason());
        // Carried through so the shipment step has a destination.
        event.put("address", text(payload, "address"));

        EventEnvelope<Object> envelope = EventEnvelope.of("payment.result", orderId, event);
        kafka.send("payment-result", orderId, mapper.writeValueAsString(envelope));
    }

    private BigDecimal amount(JsonNode payload) {
        String raw = text(payload, "amount");
        if (raw == null || raw.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("payment-request carries a non-numeric amount: " + raw, e);
        }
    }

    private PaymentMethod method(String raw) {
        if (raw == null) {
            return PaymentMethod.CREDIT_CARD;
        }
        try {
            return PaymentMethod.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return PaymentMethod.CREDIT_CARD;
        }
    }

    private JsonNode payloadOf(String message) throws Exception {
        JsonNode root = mapper.readTree(message);
        JsonNode payload = root.get("payload");
        return payload == null || payload.isNull() ? root : payload;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String required(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) {
            throw new IllegalArgumentException("payment-request is missing " + field);
        }
        return value;
    }
}

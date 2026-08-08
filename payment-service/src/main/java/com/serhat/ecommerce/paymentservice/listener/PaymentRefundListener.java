package com.serhat.ecommerce.paymentservice.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhat.ecommerce.commons.event.EventEnvelope;
import com.serhat.ecommerce.paymentservice.model.Payment;
import com.serhat.ecommerce.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Compensating step: refunds a payment when a later stage of the saga fails.
 *
 * <p>Always publishes a result, including when there was nothing to refund. The orchestrator
 * waits for {@code payment-refund-result} before considering the saga compensated, so
 * staying silent would leave it in a compensating state until the watchdog timed it out.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRefundListener {

    private final PaymentService paymentService;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;

    @KafkaListener(topics = "payment-refund", groupId = "payment-group")
    public void onRefundRequest(String message) throws Exception {
        JsonNode payload = payloadOf(message);
        String orderId = required(payload, "orderId");

        Optional<Payment> refunded = paymentService.refund(orderId);

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("orderId", orderId);
        event.put("status", refunded.map(p -> p.getStatus().name()).orElse("NOTHING_TO_REFUND"));
        refunded.ifPresent(payment -> event.put("refundedAmount", payment.getRefundedAmount()));

        EventEnvelope<Object> envelope = EventEnvelope.of("payment.refund.result", orderId, event);
        kafka.send("payment-refund-result", orderId, mapper.writeValueAsString(envelope));
    }

    private JsonNode payloadOf(String message) throws Exception {
        JsonNode root = mapper.readTree(message);
        JsonNode payload = root.get("payload");
        return payload == null || payload.isNull() ? root : payload;
    }

    private String required(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("payment-refund is missing " + field);
        }
        return value.asText();
    }
}

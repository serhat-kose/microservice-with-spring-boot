package com.serhat.ecommerce.shipmentservice.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhat.ecommerce.commons.event.EventEnvelope;
import com.serhat.ecommerce.shipmentservice.model.Shipment;
import com.serhat.ecommerce.shipmentservice.model.ShipmentStatus;
import com.serhat.ecommerce.shipmentservice.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShipmentRequestListener {

    private final ShipmentService shipmentService;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;

    @KafkaListener(topics = "shipment-request", groupId = "shipment-group")
    public void onShipmentRequest(String message) throws Exception {
        JsonNode payload = payloadOf(message);
        String orderId = required(payload, "orderId");

        Shipment shipment = shipmentService.schedule(
                orderId, text(payload, "userId"), text(payload, "address"));

        // A shipment that could not be booked is reported as a failed result, so the saga
        // refunds and releases stock rather than the order silently completing.
        boolean scheduled = shipment.getStatus() != ShipmentStatus.FAILED;

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("orderId", orderId);
        event.put("userId", shipment.getUserId());
        event.put("shipmentId", shipment.getId());
        event.put("trackingNumber", shipment.getTrackingNumber());
        event.put("carrier", shipment.getCarrier().name());
        event.put("status", scheduled ? "SCHEDULED" : "FAILED");
        event.put("reason", shipment.getFailureReason());
        event.put("estimatedDelivery", shipment.getEstimatedDelivery());

        EventEnvelope<Object> envelope = EventEnvelope.of("shipment.result", orderId, event);
        kafka.send("shipment-result", orderId, mapper.writeValueAsString(envelope));
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
            throw new IllegalArgumentException("shipment-request is missing " + field);
        }
        return value;
    }
}

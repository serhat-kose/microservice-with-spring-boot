package com.example.shipmentservice.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;

@Component
public class ShipmentRequestListener {
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper = new ObjectMapper();

    public ShipmentRequestListener(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    @KafkaListener(topics = "shipment-request", groupId = "shipment-group")
    public void onShipmentRequest(String message) throws Exception {
        Map<?,?> m = mapper.readValue(message, Map.class);
        String orderId = (String) m.get("orderId");
        // simulate shipment scheduled
        String event = mapper.writeValueAsString(Map.of(
                "orderId", orderId,
                "shipmentId", UUID.randomUUID().toString(),
                "status", "SCHEDULED"
        ));
        kafka.send("shipment-result", orderId, event);
    }
}


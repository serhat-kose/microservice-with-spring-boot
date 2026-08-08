package com.serhat.ecommerce.stockservice.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhat.ecommerce.commons.event.EventEnvelope;
import com.serhat.ecommerce.stockservice.exception.InsufficientStockException;
import com.serhat.ecommerce.stockservice.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Malformed messages and infrastructure errors are left to propagate to the shared Kafka
 * error handler, which retries and then dead-letters. Only
 * {@link InsufficientStockException} - an expected business outcome, not a failure - is
 * handled here and turned into a saga event.
 */
@Slf4j
@Component
public class StockListener {

    private final StockService stockService;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;

    public StockListener(StockService stockService, KafkaTemplate<String, String> kafka, ObjectMapper mapper) {
        this.stockService = stockService;
        this.kafka = kafka;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "stock-reserve-request", groupId = "stock-group")
    public void onReserveRequest(String message) throws Exception {
        JsonNode payload = payloadOf(message);
        String orderId = text(payload, "orderId");
        List<Map<String, Object>> items = items(payload.get("items"));

        try {
            stockService.reserveForOrder(orderId, items);

            Map<String, Object> event = new LinkedHashMap<>();
            event.put("orderId", orderId);
            event.put("userId", text(payload, "userId"));
            event.put("items", items);
            event.put("amount", payload.path("amount").asText(null));
            publish("stock-reserved", orderId, "stock.reserved", event);
        } catch (InsufficientStockException e) {
            log.warn("Stock reservation failed for order {}: {}", orderId, e.getMessage());

            Map<String, Object> event = new LinkedHashMap<>();
            event.put("orderId", orderId);
            event.put("userId", text(payload, "userId"));
            event.put("reason", "insufficient_stock");
            publish("stock-reservation-failed", orderId, "stock.reservation.failed", event);
        }
    }

    /**
     * Compensation for a failed order. Keyed by order rather than by item list, so it
     * returns exactly what was held even if the event's item list is absent or has drifted.
     */
    @KafkaListener(topics = "stock-release", groupId = "stock-group")
    public void onRelease(String message) throws Exception {
        JsonNode payload = payloadOf(message);
        String orderId = text(payload, "orderId");
        if (orderId == null) {
            throw new IllegalArgumentException("stock-release carries no orderId");
        }
        stockService.releaseForOrder(orderId);
    }

    /**
     * Marks the hold as sold once the order completes, so the TTL sweeper never hands back
     * stock for goods that were actually shipped.
     */
    @KafkaListener(topics = "order-completed", groupId = "stock-group")
    public void onOrderCompleted(String message) throws Exception {
        JsonNode payload = payloadOf(message);
        String orderId = text(payload, "orderId");
        if (orderId != null) {
            stockService.confirmForOrder(orderId);
        }
    }

    /** Removes stock rows for products that have been withdrawn from the catalog. */
    @KafkaListener(topics = "product-deleted", groupId = "stock-group")
    public void onProductDeleted(String message) throws Exception {
        JsonNode payload = payloadOf(message);
        JsonNode productId = payload.get("productId");
        if (productId != null && !productId.isNull()) {
            stockService.deleteQuietly(String.valueOf(productId.asLong()));
        }
    }

    private void publish(String topic, String key, String eventType, Map<String, Object> payload)
            throws Exception {
        EventEnvelope<Object> envelope = EventEnvelope.of(eventType, null, payload);
        kafka.send(topic, key, mapper.writeValueAsString(envelope));
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

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> items(JsonNode node) {
        if (node == null || !node.isArray()) {
            return new ArrayList<>();
        }
        return mapper.convertValue(node, List.class);
    }
}
